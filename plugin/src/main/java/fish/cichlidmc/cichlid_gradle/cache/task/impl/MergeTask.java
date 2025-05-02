package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import fish.cichlidmc.cichlid_gradle.merge.JarMerger;
import fish.cichlidmc.cichlid_gradle.merge.MergeSource;
import fish.cichlidmc.cichlid_gradle.util.Distribution;

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
		this.context.submit(new DecompileTask(this.context, Distribution.MERGED, this.output));
	}
}
