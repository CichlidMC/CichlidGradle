package io.github.cichlidmc.cichlid_gradle.extension;

import javax.inject.Inject;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.extension.minecraft.MinecraftExtension;
import io.github.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import org.gradle.api.Project;

public abstract class CichlidExtensionImpl implements CichlidExtension {
	private final MinecraftExtension mc;

	@Inject
	public CichlidExtensionImpl(Project project) {
		this.mc = this.getExtensions().create("minecraft", MinecraftExtension.class);
		project.afterEvaluate(this::apply);
	}

	private void apply(Project project) {
		String mcVer = this.mc.getVersion().get();
		CichlidCache cache = CichlidCache.get(project);
		cache.maven.ensureVersionDownloaded(mcVer);

		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft");
			repo.setUrl(cache.maven.root);
		});

		String mcDist = this.mc.getDistribution().get();
		project.getDependencies().add("implementation", "net.minecraft:minecraft-" + mcDist + ':' + mcVer);

		RunTaskGeneration.run(mcVer, cache.runs, project);
	}
}
