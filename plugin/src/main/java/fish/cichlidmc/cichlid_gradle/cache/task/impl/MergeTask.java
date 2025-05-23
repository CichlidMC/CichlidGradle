package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.JarsStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.merge.JarMerger;
import fish.cichlidmc.cichlid_gradle.merge.MergeSource;
import fish.cichlidmc.distmarker.Dist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MergeTask extends CacheTask {
	protected MergeTask(CacheTaskEnvironment env) {
		super("Merge", env);
	}

	@Override
	protected void doRun() throws IOException {
		JarsStorage jars = this.env.cache.getVersion(this.env.version.id).jars;

		List<CompletableFuture<MergeSource>> futures = new ArrayList<>();

		for (Dist dist : Dist.values()) {
			Path jar = jars.get(dist);
			if (Files.exists(jar)) {
				futures.add(CompletableFuture.completedFuture(new MergeSource(dist, jar)));
			} else {
				futures.add(this.env.submit(null).thenApply(_ -> MergeSource.createUnchecked(dist, jar)));
			}
		}

		List<MergeSource> sources = futures.stream().map(CompletableFuture::join).toList();
		JarMerger.merge(sources, jars.merged());
	}
}
