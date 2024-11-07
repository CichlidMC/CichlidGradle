package io.github.cichlidmc.cichlid_gradle;

import java.util.Objects;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.cache.MinecraftMaven;
import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import io.github.cichlidmc.cichlid_gradle.extension.dep.CichlidDepsExtension;
import io.github.cichlidmc.cichlid_gradle.extension.repo.CichlidReposExtension;
import io.github.cichlidmc.cichlid_gradle.extension.dep.MinecraftDepsExtension;
import io.github.cichlidmc.cichlid_gradle.extension.repo.MinecraftReposExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

public abstract class CichlidGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		CichlidExtension.setup(project);
		CichlidReposExtension.setup(project);
		MinecraftReposExtension.setup(project);
		CichlidDepsExtension.setup(project);
		MinecraftDepsExtension.setup(project);

		// listen for all minecraft dependencies to ensure they're downloaded
		CichlidCache cache = CichlidCache.get(project);
		project.getConfigurations().configureEach(
				configuration -> configuration.getDependencies().configureEach(dep -> {
					if (isMinecraftDependency(dep)) {
						String version = Objects.requireNonNull(dep.getVersion());
						cache.maven.ensureVersionDownloaded(version);
					}
				})
		);
	}

	private static boolean isMinecraftDependency(Dependency dep) {
		return "net.minecraft".equals(dep.getGroup())
				&& MinecraftMaven.MC_MODULES.contains(dep.getName())
				&& dep.getVersion() != null;
	}
}
