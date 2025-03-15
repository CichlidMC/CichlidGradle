package io.github.cichlidmc.cichlid_gradle.cache.task;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TaskContext {
	private static final Logger logger = Logging.getLogger(TaskContext.class);

	private final Map<CacheTask, CompletableFuture<Void>> futures = Collections.synchronizedMap(new IdentityHashMap<>());
	private final Set<CacheTask> incompleteTasks = Collections.synchronizedSet(new HashSet<>());
	private final Map<CacheTask, Throwable> errors = Collections.synchronizedMap(new IdentityHashMap<>());

	public CompletableFuture<Void> submit(CacheTask task) {
		logger.quiet("Starting new task: {} - {}", task.name, task.description);

		CompletableFuture<Void> future = CompletableFuture.runAsync(task)
				.thenRun(() -> this.finishTask(task))
				.exceptionally(error -> {
					this.finishTask(task);
					this.errors.put(task, error);
					return null;
				});

		this.futures.put(task, future);
		this.incompleteTasks.add(task);
		return future;
	}

	public void join() {
		while (!this.incompleteTasks.isEmpty()) {
			CacheTask task = this.incompleteTasks.iterator().next();
			CompletableFuture<Void> future = this.futures.get(task);
			try {
				future.join();
			} catch (Throwable ignored) {}
		}
	}

	public void report() {
		this.join();

		if (this.errors.isEmpty()) {
			logger.quiet("All tasks finished successfully.");
			return;
		}

		RuntimeException root = new RuntimeException("One or more CichlidGradle tasks failed!");
		this.errors.values().forEach(root::addSuppressed);

		logger.error(root.getMessage(), root);
		throw root;
	}

	private void finishTask(CacheTask task) {
		logger.quiet("Task complete: {}", task.name);
		this.incompleteTasks.remove(task);
	}
}
