package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.mcmaven.JarProcessor;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.util.Utils;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReassembleBinaryTask extends CacheTask {
	private boolean ranTransform;

	public ReassembleBinaryTask(CacheTaskEnvironment env) {
		super("Reassemble binary jar", env);
	}

	@Override
	protected void doRun() throws IOException {
		Path input = this.env.cache.getVersion(this.env.version.id).jars.get(this.env.dist);

		if (!Files.exists(input)) {
			this.env.submitAndAwait(this.env.dist::createSetupTask);
		}

		Path output = this.env.cache.reassembledJars.binary(this.env.version.id, this.env.transformers.hash(), this.env.dist);

		JarProcessor.run(input, output, this::transform);
	}

	private JarProcessor.ClassGroup transform(JarProcessor.ClassGroup group) throws IOException {
		return new JarProcessor.ClassGroup(this.transform(group.main()), Utils.map(group.inner(), this::transform));
	}

	private JarProcessor.ClassEntry transform(JarProcessor.ClassEntry entry) throws IOException {
		if (entry.fileName().endsWith("package-info.class"))
			return entry;

		String bytecodeHash = Encoding.BASE_FUNNY.encode(HashAlgorithm.SHA256.hash(entry.content()));
		Path transformed = this.env.cache.transformedClasses.get(this.env.transformers.hash(), bytecodeHash);

		if (!Files.exists(transformed)) {
			if (this.ranTransform) {
				throw new IllegalStateException("Class is still missing after transform: " + entry.fileName());
			}

			this.ranTransform = true;
			this.env.submitAndAwait(TransformTask::new);

			// let's try that again
			if (!Files.exists(transformed)) {
				throw new IllegalStateException("Class is still missing after transform: " + entry.fileName());
			}
		}

		return new JarProcessor.ClassEntry(entry.fileName(), transformed);
	}
}
