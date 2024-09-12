package io.github.cichlidmc.cichlid_gradle.extension;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.extension.minecraft.MinecraftExtension;
import io.github.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
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

		CichlidCache cache = CichlidCache.get(project);
		project.getRepositories().maven(repo -> {
			repo.setName("Minecraft");
			repo.setUrl(cache.maven.root);
		});

		MinecraftExtension mc = MinecraftExtension.get(project);
		String mcVer = mc.getVersion();
		if (mcVer == null)
			return;

		Distribution dist = mc.getDist();
		if (dist == null) {
			throw new InvalidUserDataException("Minecraft version is set but distribution is not");
		}

		cache.maven.ensureVersionDownloaded(mcVer);

		String dep = "net.minecraft:minecraft-" + dist + ':' + mcVer;
		project.getDependencies().add("implementation", dep);

		RunTaskGeneration.run(mcVer, cache.runs, project);
	}
}
