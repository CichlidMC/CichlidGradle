package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.storage.LockableStorage;
import io.github.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.merge.MergeSource;
import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.distmarker.Dist;
import io.github.cichlidmc.pistonmetaparser.FullVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
					  init ------+------> SetupTask
								 |		     / \
								 V		    /   \
							AssetsTask	   /     \
										  /       \
					   Client SetupDistTask		  Server SetupDistTask
							|									|
 generate run template <----+----> generate metadata			+----> generate metadata
							+----> extract natives				|
							|									|
						   / \								   / \
						  /   \								  /   \
		  download mappings   download jar	 	   download jar   download mappings
						  \   /					  			  \   /
						   \ /					  			   \ /
							|									|
							|								unbundle
							|									|
							|									+----> generate bundler metadata
							|									|
							V									V
						  Remap								  Remap
							|								    |
		save remap log <----+----> decompile	 decompile <----+----> save remap log
							|								    +----> generate run template
							|									|
							+-----------------+-----------------+
											  |
								   merge <----+----> generate merged metadata
 */
public class SetupTask extends CacheTask {
	private final VersionStorage storage;
	private final FullVersion version;

	private LockableStorage.Lock lock;

	public SetupTask(TaskContext context, VersionStorage storage, FullVersion version) {
		super("Setup", "Setup for " + version.id, context);
		this.storage = storage;
		this.version = version;
	}

	@Override
	protected void doRun() throws IOException {
		this.lock = this.storage.lock();
		if (this.storage.isComplete())
			return;

		if (Files.exists(this.storage.root)) {
			this.logger.warn("Found existing incomplete files for version {}, overwriting", this.version.id);
			FileUtils.deleteRecursively(this.storage.root);
		}

		CompletableFuture<Void> clientFuture = this.context.submit(this.createSetupTask(Distribution.CLIENT));

		if (this.version.downloads.server.isPresent()) {
			CompletableFuture<Void> serverFuture = this.context.submit(this.createSetupTask(Distribution.SERVER));

			// we know there will be a merged jar, might as well generate the metadata for it now
			this.context.submit(new GenerateMetadataTask(this.context, Distribution.MERGED, this.storage.jars, this.version));

			// now that both have started, wait for both to finish
			CompletableFuture.allOf(clientFuture, serverFuture).join();

			MergeTask mergeTask = this.createMergeTask();
			this.context.submit(mergeTask);
		}
	}

	private SetupDistTask createSetupTask(Distribution dist) {
		return new SetupDistTask(this.context, dist, this.storage, this.version);
	}

	@Override
	protected void cleanup() throws IOException {
		if (this.lock != null) {
			this.lock.close();
		}
	}

	private MergeTask createMergeTask() throws IOException {
		List<MergeSource> sources = new ArrayList<>();

		Path clientJar = this.storage.jars.path(Distribution.CLIENT);
		sources.add(new MergeSource(Dist.CLIENT, clientJar));
		Path serverJar = this.storage.jars.path(Distribution.SERVER);
		sources.add(new MergeSource(Dist.SERVER, serverJar));

		Path bundlerJar = this.storage.jars.path(Distribution.BUNDLER);
		if (Files.exists(bundlerJar)) {
			sources.add(new MergeSource(Dist.BUNDLER, bundlerJar));
		}

		Path mergedJar = this.storage.jars.path(Distribution.MERGED);
		return new MergeTask(this.context, sources, mergedJar);
	}
}
