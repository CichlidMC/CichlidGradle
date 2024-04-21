package io.github.tropheusj.cichlid_gradle;

import io.github.tropheusj.cichlid_gradle.extension.CichlidExtension;
import io.github.tropheusj.cichlid_gradle.extension.CichlidExtensionImpl;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CichlidGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		CichlidExtension extension = project.getExtensions().create(
				CichlidExtension.class, "cichlid", CichlidExtensionImpl.class
		);

	}
}
