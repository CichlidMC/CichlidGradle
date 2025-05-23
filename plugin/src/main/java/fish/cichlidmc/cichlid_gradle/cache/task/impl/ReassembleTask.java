package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;

import java.io.IOException;
import java.nio.file.Path;

public final class ReassembleTask extends CacheTask {
	private final boolean sources;

	public ReassembleTask(CacheTaskEnvironment env, boolean sources) {
		super("Reassemble " + (sources ? "sources" : "binary"), env);
		this.sources = sources;
	}

	@Override
	protected void doRun() throws IOException {
		Path output = this.env.cache.reassembledJars.get(this.env.version.id, this.env.transformers.hash(), this.env.dist, this.sources);
		Path input = this.env.cache.
	}
}
