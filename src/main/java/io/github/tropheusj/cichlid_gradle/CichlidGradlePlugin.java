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
	private final RepositoryTransportFactory repositoryTransportFactory;

	@Inject
	public CichlidGradlePlugin(RepositoryTransportFactory repositoryTransportFactory) {
		this.repositoryTransportFactory = repositoryTransportFactory;
	}

	@Override
	public void apply(Project project) {
		McMavenConnectorFactory.inject(repositoryTransportFactory, project.getGradle().getGradleUserHomeDir());

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
