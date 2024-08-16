package io.github.cichlidmc.cichlid_gradle.extension;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.extension.minecraft.MinecraftExtension;
import io.github.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import org.gradle.api.Action;
import org.gradle.api.Project;

public abstract class CichlidExtensionImpl implements CichlidExtension {
	private final List<Action<? super Project>> beforeApplyActions = new ArrayList<>();

	@Inject
	public CichlidExtensionImpl(Project project) {
		project.afterEvaluate(this::apply);
	}

	@Override
	public void beforeApply(Action<? super Project> action) {
		this.beforeApplyActions.add(action);
	}

	private void apply(Project project) {
		//noinspection ForLoopReplaceableByForEach - used so adding more to the end while iterating is safe
		for (int i = 0; i < this.beforeApplyActions.size(); i++) {
			this.beforeApplyActions.get(i).execute(project);
		}

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
