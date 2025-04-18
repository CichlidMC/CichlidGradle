package io.github.cichlidmc.cichlid_gradle.transform;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class MinecraftSourcesTransformAction implements TransformAction<TransformParameters.None> {
	@InputArtifact
	@PathSensitive(PathSensitivity.NAME_ONLY)
	public abstract Provider<FileSystemLocation> getInput();

	@Override
	public void transform(TransformOutputs outputs) {
		File input = this.getInput().get().getAsFile();
		System.out.println("transforming sources! " + input);

		String name = input.getName().replace(".jar", "-transformed.jar");
		File output = outputs.file(name);

		try {
			Files.copy(input.toPath(), output.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
