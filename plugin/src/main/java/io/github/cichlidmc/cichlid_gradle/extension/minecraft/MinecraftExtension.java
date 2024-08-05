package io.github.cichlidmc.cichlid_gradle.extension.minecraft;

import org.gradle.api.Project;

public class MinecraftExtension {
	// these cannot be managed properties because DependencyHandler extensions are not given an ObjectFactory.
	// https://github.com/gradle/gradle/issues/18213
	private String version;
	private String distribution;

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDistribution() {
		return this.distribution;
	}

	public void setDistribution(String distribution) {
		this.distribution = distribution;
	}

	public static void setup(Project project) {
		project.getDependencies().getExtensions().create("minecraft", MinecraftExtension.class);
	}

	public static MinecraftExtension get(Project project) {
		return project.getDependencies().getExtensions().getByType(MinecraftExtension.class);
	}
}
