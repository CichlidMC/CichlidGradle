package io.github.cichlidmc.cichlid_gradle.extension;

import io.github.cichlidmc.cichlid_gradle.run.RunConfiguration;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

public interface CichlidExtension {
	NamedDomainObjectContainer<RunConfiguration> getRuns();

	static void setup(Project project) {
		project.getExtensions().create(CichlidExtension.class, "cichlid", CichlidExtensionImpl.class);
	}

	static CichlidExtension get(Project project) {
		return project.getExtensions().getByType(CichlidExtension.class);
	}
}
