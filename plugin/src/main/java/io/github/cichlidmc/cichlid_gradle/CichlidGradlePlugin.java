package io.github.cichlidmc.cichlid_gradle;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import io.github.cichlidmc.cichlid_gradle.extension.dep.CichlidDepsExtension;
import io.github.cichlidmc.cichlid_gradle.extension.dep.MinecraftDepsExtension;
import io.github.cichlidmc.cichlid_gradle.extension.repo.CichlidReposExtension;
import io.github.cichlidmc.cichlid_gradle.extension.repo.MinecraftReposExtension;
import io.github.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import java.util.Objects;

public abstract class CichlidGradlePlugin implements Plugin<Project> {
	public static final String NAME = "CichlidGradle";
	public static final String VERSION = "1.0-SNAPSHOT";

	@Override
	public void apply(Project project) {
		CichlidExtension.setup(project);
		CichlidReposExtension.setup(project);
		MinecraftReposExtension.setup(project);
		CichlidDepsExtension.setup(project);
		MinecraftDepsExtension.setup(project);
		RunTaskGeneration.setup(project);

		// listen for all minecraft dependencies to ensure they're downloaded
		project.getConfigurations().configureEach(
				configuration -> configuration.getDependencies().configureEach(dep -> {
					if (isMinecraftDependency(dep)) {
						String version = Objects.requireNonNull(dep.getVersion());
						CichlidCache cache = CichlidCache.get(project);
						cache.ensureVersionIsCached(version);
					}
				})
		);
	}

	private static boolean isMinecraftDependency(Dependency dep) {
		return "net.minecraft".equals(dep.getGroup())
				&& CichlidCache.MINECRAFT_MODULES.contains(dep.getName())
				&& dep.getVersion() != null;
	}
}
