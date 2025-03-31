package io.github.cichlidmc.cichlid_gradle.transform;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.InputArtifactDependencies;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// @CacheableTransform
public abstract class SushiTransformAction implements TransformAction<TransformParameters.None> {
	private static final Logger logger = Logging.getLogger(SushiTransformAction.class);

	@InputArtifact
	@PathSensitive(PathSensitivity.NAME_ONLY)
	public abstract Provider<FileSystemLocation> getInput();

	@InputArtifactDependencies
	@PathSensitive(PathSensitivity.NAME_ONLY)
	public abstract FileCollection getDependencies();

	@Override
	public void transform(TransformOutputs outputs) {
		File file = this.getInput().get().getAsFile();
		String name = file.getName();

		if (!name.endsWith(".jar") || true) {
			File output = outputs.file(file);
			copy(file, output);
			return;
		}

		logger.quiet("transformed " + name);
		for (File dep : getDependencies()) {
			logger.quiet("\t- " + dep.getName());
		}

		String newName = name.replace(".jar", "-transformed.jar");
		File output = outputs.file(newName);
		copy(file, output);
	}

	private static void copy(File from, File to) {
		try {
			Files.copy(from.toPath(), to.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
