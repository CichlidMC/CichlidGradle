package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.merge.JarMerger;
import io.github.cichlidmc.cichlid_gradle.merge.MergeSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MergeTask extends CacheTask {
	private final List<MergeSource> sources;
	private final Path output;

	protected MergeTask(TaskContext context, List<MergeSource> sources, Path output) {
		super("Merge", "Merging " + sources, context);
		this.sources = sources;
		this.output = output;
	}

	@Override
	protected void doRun() throws IOException {
		JarMerger.merge(this.sources, this.output);
		// TODO: decompile
	}
}
