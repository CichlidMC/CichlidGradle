package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.mcmaven.JarProcessor;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReassembleSourcesTask extends CacheTask {
	private boolean ranDecompile;

	public ReassembleSourcesTask(CacheTaskEnvironment env) {
		super("Reassemble sources jar", env);
	}

	@Override
	protected String run() throws IOException {
		Path output = this.env.cache.reassembledJars.sources(this.env.version.id, this.env.transformers.hash(), this.env.dist);
		Path binary = this.env.cache.reassembledJars.binary(this.env.version.id, this.env.transformers.hash(), this.env.dist);
		if (!Files.exists(binary)) {
			// reassemble the binary and wait for it
			this.env.submitAndAwait(ReassembleBinaryTask::new);
			FileUtils.assertExists(binary);
		}

		JarProcessor.run(binary, output, this::getDecompiled);
		return null;
	}

	private JarProcessor.ClassGroup getDecompiled(JarProcessor.ClassGroup group) throws IOException {
		String hash = group.hash();
		Path path = this.env.cache.decompiledClasses.get(hash);

		if (!Files.exists(path)) {
			if (this.ranDecompile) {
				throw new IllegalStateException("Group is still missing after decompile: " + group.main().fileName());
			}

			this.ranDecompile = true;
			this.env.submitAndAwait(DecompileTask::new);

			// let's try that again
			if (!Files.exists(path)) {
				throw new IllegalStateException("Group is still missing after decompile: " + group.main().fileName());
			}
		}

		String newName = group.main().fileName().replace(".class", ".java");
		return new JarProcessor.ClassGroup(new JarProcessor.ClassEntry(newName, path));
	}
}
