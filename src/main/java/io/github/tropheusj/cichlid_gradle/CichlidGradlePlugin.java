package io.github.tropheusj.cichlid_gradle;

import java.net.URI;

import io.github.tropheusj.cichlid_gradle.extension.CichlidExtension;
import io.github.tropheusj.cichlid_gradle.extension.CichlidExtensionImpl;
import io.github.tropheusj.cichlid_gradle.minecraft.mcmaven.McMavenConnectorFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;

import javax.inject.Inject;


public abstract class CichlidGradlePlugin implements Plugin<Project> {
	@Inject
	public CichlidGradlePlugin(RepositoryTransportFactory repositoryTransportFactory) {
		McMavenConnectorFactory.inject(repositoryTransportFactory);
	}

	@Override
	public void apply(Project project) {
		CichlidExtension extension = project.getExtensions().create(
				CichlidExtension.class, "cichlid", CichlidExtensionImpl.class
		);

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft Libraries");
			repo.setUrl(URI.create("https://libraries.minecraft.net/"));
		});

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft");
            repo.setUrl(URI.create("mcmaven://aaa"));
        });
	}
}
