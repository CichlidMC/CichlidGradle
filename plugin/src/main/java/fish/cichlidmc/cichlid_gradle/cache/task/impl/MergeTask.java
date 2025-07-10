package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.JarsStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.merge.JarMerger;
import fish.cichlidmc.cichlid_gradle.merge.MergeSource;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MergeTask extends CacheTask {
	public MergeTask(CacheTaskEnvironment env) {
		super("Merge", env);
	}

	@Override
	protected String run() throws IOException {
		JarsStorage jars = this.env.cache.getVersion(this.env.version.id).jars;

		List<CompletableFuture<@Nullable MergeSource>> futures = new ArrayList<>();

		futures.add(this.createFuture(Distribution.CLIENT, jars));
		CompletableFuture<MergeSource> server = this.createFuture(Distribution.SERVER, jars);
		futures.add(server);

		// need to wait for the server to be done before checking for the bundler
		futures.add(server.thenApply(ignored -> {
			Path bundler = jars.get(Dist.BUNDLER);
			return Files.exists(bundler) ? MergeSource.createUnchecked(Dist.BUNDLER, bundler) : null;
		}));

		List<MergeSource> sources = futures.stream()
				.map(CompletableFuture::join)
				.filter(Objects::nonNull)
				.toList();

		JarMerger.merge(sources, jars.get(Distribution.MERGED));
		return "Merged " + sources.size() + " sources: " + sources;
	}

	private CompletableFuture<MergeSource> createFuture(Distribution dist, JarsStorage jars) throws IOException {
		Dist asMarkerDist = switch (dist) {
			case CLIENT -> Dist.CLIENT;
			case SERVER -> Dist.SERVER;
			case MERGED -> throw new IllegalArgumentException("MERGED is not allowed");
		};

		Path jar = jars.get(dist);
		if (Files.exists(jar)) {
			return CompletableFuture.completedFuture(new MergeSource(asMarkerDist, jar));
		} else {
			CacheTaskEnvironment env = this.env.withDist(dist);
			return env.submit(SetupTask::new).thenApply($ -> MergeSource.createUnchecked(asMarkerDist, jar));
		}
	}
}
