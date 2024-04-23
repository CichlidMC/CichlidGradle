package io.github.tropheusj.cichlid_gradle;

import io.github.tropheusj.cichlid_gradle.extension.CichlidExtension;
import io.github.tropheusj.cichlid_gradle.extension.CichlidExtensionImpl;
import io.github.tropheusj.cichlid_gradle.minecraft.MinecraftMaven;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CichlidGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		CichlidExtension extension = project.getExtensions().create(
				CichlidExtension.class, "cichlid", CichlidExtensionImpl.class
		);

		MinecraftMaven.ensureDummied(project.getGradle());

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft");
            repo.setUrl(MinecraftMaven.getUri(project.getGradle()));
        });

		project.getConfigurations().all(configuration -> {
			configuration.resolutionStrategy(strategy -> {
				strategy.eachDependency(dep -> dep.)
			})
		});
	}
}
