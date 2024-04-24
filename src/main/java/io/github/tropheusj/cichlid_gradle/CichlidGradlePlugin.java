package io.github.tropheusj.cichlid_gradle;

import java.net.URI;

import javax.inject.Inject;

import io.github.tropheusj.cichlid_gradle.extension.CichlidExtension;
import io.github.tropheusj.cichlid_gradle.extension.CichlidExtensionImpl;
import io.github.tropheusj.cichlid_gradle.minecraft.MinecraftMaven;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;

public class CichlidGradlePlugin implements Plugin<Project> {
	@Inject
	RepositoryTransportFactory hh;

	@Override
	public void apply(Project project) {
		CichlidExtension extension = project.getExtensions().create(
				CichlidExtension.class, "cichlid", CichlidExtensionImpl.class
		);

		hh.toString();

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft");
            repo.setUrl(URI.create("mcmaven://aaa"));
        });
	}
}
