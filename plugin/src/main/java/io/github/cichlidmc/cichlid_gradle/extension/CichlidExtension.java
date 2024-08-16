package io.github.cichlidmc.cichlid_gradle.extension;

import io.github.cichlidmc.cichlid_gradle.run.RunConfiguration;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

public interface CichlidExtension {
	NamedDomainObjectContainer<RunConfiguration> getRuns();

	/**
	 * Actions registered here are invoked right before Cichlid applies the Minecraft maven and dependency.
	 * It is safe to register additional actions in callbacks, matching the behavior of {@link Project#afterEvaluate(Action)}.
	 * This may be useful in niche situations.
	 */
	void beforeApply(Action<? super Project> action);

	static void setup(Project project) {
		project.getExtensions().create(CichlidExtension.class, "cichlid", CichlidExtensionImpl.class);
	}

	static CichlidExtension get(Project project) {
		return project.getExtensions().getByType(CichlidExtension.class);
	}
}
