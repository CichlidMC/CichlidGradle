package io.github.tropheusj.cichlid_gradle;

import io.github.tropheusj.cichlid_gradle.pistonmeta.VersionManifest;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CichlidGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		VersionManifest fetch = VersionManifest.fetch();
		System.out.println("manifest: " + fetch);
//		project.getRepositories().add()
	}
}
