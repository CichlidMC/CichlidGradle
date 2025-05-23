package fish.cichlidmc.cichlid_gradle.cache.task;

import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.cache.Transformers;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CacheTaskEnvironment {
	private static final Logger logger = Logging.getLogger(CacheTaskEnvironment.class);

	public final FullVersion version;
	public final CichlidCache cache;
	public final Distribution dist;
	public final Transformers transformers;

	private final Map<CacheTask, CompletableFuture<Void>> futures = Collections.synchronizedMap(new IdentityHashMap<>());
	private final Set<CacheTask> incompleteTasks = Collections.synchronizedSet(new HashSet<>());
	private final Map<CacheTask, Throwable> errors = Collections.synchronizedMap(new IdentityHashMap<>());

	public CacheTaskEnvironment(FullVersion version, CichlidCache cache, Distribution dist, Transformers transformers) {
		this.version = version;
		this.cache = cache;
		this.dist = dist;
		this.transformers = transformers;
	}

	public void submitAndAwait(TaskFactory factory) {
		this.submit(factory).join();
	}

	public CompletableFuture<Void> submit(TaskFactory factory) {
		return this.submit(factory.create(this));
	}

	public CompletableFuture<Void> submit(CacheTask task) {
		logger.quiet("Starting new task: {}", task.name);

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

		RuntimeException root = new RuntimeException("One or more CichlidGradle cache tasks failed!");
		this.errors.values().forEach(root::addSuppressed);

		logger.error(root.getMessage(), root);
		throw root;
	}

	private void finishTask(CacheTask task) {
		this.incompleteTasks.remove(task);
		logger.quiet("Task complete: {}", task.name);
	}

	@FunctionalInterface
	public interface TaskFactory {
		CacheTask create(CacheTaskEnvironment env);
	}

	public static final class Builder {
		private final FullVersion version;
		private final CichlidCache cache;
		private final Distribution dist;
		private final Transformers transformers;
		private final List<TaskFactory> factories;

		public Builder(FullVersion version, CichlidCache cache, Distribution dist, Transformers transformers) {
			this.version = version;
			this.cache = cache;
			this.dist = dist;
			this.transformers = transformers;
			this.factories = new ArrayList<>();
		}

		public void add(TaskFactory factory) {
			this.factories.add(factory);
		}

		public CacheTaskEnvironment start() {
			logger.quiet("Starting {} cache task(s)...", this.factories.size());
			CacheTaskEnvironment env = new CacheTaskEnvironment(this.version, this.cache, this.dist, this.transformers);
			this.factories.forEach(env::submit);
			return env;
		}
	}
}
