package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.cache.task.processor.JarProcessor;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.cichlid_gradle.util.io.WorkFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

public final class ReassembleBinaryTask extends CacheTask {
	public ReassembleBinaryTask(CacheTaskEnvironment env) {
		super("Reassemble binary jar", env);
	}

	@Override
	protected String run() throws IOException {
		Path output = this.env.cache.reassembledJars.binary(this.env.version.id, this.env.hash, this.env.dist);
		Path input = this.env.cache.getVersion(this.env.version.id).jars.get(this.env.dist);

		if (!Files.exists(input)) {
			this.env.submitAndAwait(this.env.dist::createSetupTask);
			FileUtils.assertExists(input);
		}

		WorkFile.doIfEmpty(output, file -> {
			try (ZipOutputStream outputStream = new ZipOutputStream(file.newOutputStream())) {
				JarProcessor.run(input, outputStream, ClassTransformer.create(this.env));
			}
		});
		return null;
	}
}
