package io.github.cichlidmc.cichlid_gradle;

import java.net.URI;

import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtensionImpl;
import io.github.cichlidmc.cichlid_gradle.minecraft.mcmaven.McMavenConnectorFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public abstract class CichlidGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		McMavenConnectorFactory.inject(project);

		CichlidExtension extension = project.getExtensions().create(
				CichlidExtension.class, "cichlid", CichlidExtensionImpl.class
		);

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft Libraries");
			repo.setUrl(URI.create("https://libraries.minecraft.net/"));
		});

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft");
            repo.setUrl(URI.create("mcmaven:///"));
        });
	}
}
