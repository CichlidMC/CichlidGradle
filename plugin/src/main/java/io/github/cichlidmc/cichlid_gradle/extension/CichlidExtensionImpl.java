package io.github.cichlidmc.cichlid_gradle.extension;

import javax.inject.Inject;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.extension.minecraft.MinecraftExtension;
import io.github.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import org.gradle.api.Project;

public abstract class CichlidExtensionImpl implements CichlidExtension {
	@Inject
	public CichlidExtensionImpl(Project project) {
		project.afterEvaluate(this::apply);
	}

	private void apply(Project project) {
		MinecraftExtension mc = MinecraftExtension.get(project);
		String mcVer = mc.getVersion();
		CichlidCache cache = CichlidCache.get(project);
		cache.maven.ensureVersionDownloaded(mcVer);

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft");
			repo.setUrl(cache.maven.root);
		});

		String mcDist = mc.getDistribution();
		project.getDependencies().add("implementation", "net.minecraft:minecraft-" + mcDist + ':' + mcVer);

		RunTaskGeneration.run(mcVer, cache.runs, project);
	}
}
