package io.github.cichlidmc.cichlid_gradle.extension.minecraft;

import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.Project;

public class MinecraftExtension {
	// this cannot be a managed property because DependencyHandler extensions are not given an ObjectFactory.
	// https://github.com/gradle/gradle/issues/18213
	private String version;

	private Distribution dist;

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void client() {
		this.dist = Distribution.CLIENT;
	}

	public void server() {
		this.dist = Distribution.SERVER;
	}

	public void clientAndServer() {
		this.dist = Distribution.MERGED;
	}

	public Distribution getDist() {
		return dist;
	}

	public static void setup(Project project) {
		project.getDependencies().getExtensions().create("minecraft", MinecraftExtension.class);
	}

	public static MinecraftExtension get(Project project) {
		return project.getDependencies().getExtensions().getByType(MinecraftExtension.class);
	}
}
