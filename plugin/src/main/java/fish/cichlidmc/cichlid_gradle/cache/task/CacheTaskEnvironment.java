package fish.cichlidmc.cichlid_gradle.cache.task;

import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.extension.def.TransformersImpl;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.tinycodecs.util.Either;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

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

	public final String hash;
	public final FullVersion version;
	public final CichlidCache cache;
	public final Distribution dist;
	public final TransformersImpl transformers;

	private final Map<CacheTask.Runner, CompletableFuture<Void>> futures = Collections.synchronizedMap(new IdentityHashMap<>());
	private final Set<CacheTask.Runner> incompleteTasks = Collections.synchronizedSet(new HashSet<>());
	private final Map<CacheTask.Runner, Throwable> errors = Collections.synchronizedMap(new IdentityHashMap<>());

	public CacheTaskEnvironment(String hash, FullVersion version, CichlidCache cache, Distribution dist, TransformersImpl transformers) {
		this.hash = hash;
		this.version = version;
		this.cache = cache;
		this.dist = dist;
		this.transformers = transformers;
	}

	public CacheTaskEnvironment withDist(Distribution dist) {
		return this.dist == dist ? this : new CacheTaskEnvironment(this.hash, this.version, this.cache, dist, this.transformers);
	}

	public void submitAndAwait(TaskFactory factory) {
		this.submit(factory).join();
	}

	public CompletableFuture<Void> submit(TaskFactory factory) {
		return this.submit(factory.create(this));
	}

	public CompletableFuture<Void> submit(CacheTask task) {
		logger.quiet("Starting new task: {}", task.name);

		CacheTask.Runner runner = new CacheTask.Runner(task);
		this.incompleteTasks.add(runner);

		CompletableFuture<Void> future = CompletableFuture.supplyAsync(runner)
				.thenAccept(message -> this.finishTask(runner, Either.left(message)))
				.exceptionally(error -> {
					this.finishTask(runner, Either.right(error));
					return null;
				});

		this.futures.put(runner, future);

		return future;
	}

	public void join() {
		while (!this.incompleteTasks.isEmpty()) {
			CacheTask.Runner runner = this.incompleteTasks.iterator().next();
			CompletableFuture<Void> future = this.futures.get(runner);
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

		RuntimeException root = new RuntimeException(this.errors.size() + " CichlidGradle cache task(s) failed!");
		this.errors.values().forEach(root::addSuppressed);

		logger.error(root.getMessage(), root);
		throw root;
	}

	private void finishTask(CacheTask.Runner runner, Either<@Nullable String, Throwable> result) {
		this.incompleteTasks.remove(runner);

		if (result.isRight()) {
			this.errors.put(runner, result.right());
		}

		String name = runner.task().name;
		String verb = result.isLeft() ? "finished" : "failed";
		String seconds = String.format("%.2f", (System.currentTimeMillis() - runner.startTime()) / 1000f);
		String message = result.isLeft() ? result.left() : result.right().getMessage();

		if (message != null) {
			logger.quiet("Task '{}' {} after {} seconds: {}", name, verb, seconds, message);
		} else {
			logger.quiet("Task '{}' {} after {} seconds", name, verb, seconds);
		}
	}

	@FunctionalInterface
	public interface TaskFactory {
		CacheTask create(CacheTaskEnvironment env);
	}

	public static final class Builder {
		private final String hash;
		private final FullVersion version;
		private final CichlidCache cache;
		private final Distribution dist;
		private final TransformersImpl transformers;
		private final List<TaskFactory> factories;

		public Builder(String hash, FullVersion version, CichlidCache cache, Distribution dist, TransformersImpl transformers) {
			this.hash = hash;
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
			CacheTaskEnvironment env = new CacheTaskEnvironment(this.hash, this.version, this.cache, this.dist, this.transformers);
			this.factories.forEach(env::submit);
			return env;
		}
	}
}
