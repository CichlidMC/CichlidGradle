package io.github.cichlidmc.cichlid_gradle.extension.dep;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class MinecraftDepsExtension {
	// can't use managed properties - https://github.com/gradle/gradle/issues/18213
	private final DependencyHandler deps;

    private MinecraftDepsExtension(DependencyHandler deps) {
        this.deps = deps;
    }

    public Dependency client(String version) {
		return this.deps.create("net.minecraft:minecraft-client:" + version);
	}

	public Dependency server(String version) {
		return this.deps.create("net.minecraft:minecraft-server:" + version);
	}

	public Dependency clientAndServer(String version) {
		return this.deps.create("net.minecraft:minecraft-merged:" + version);
	}

	public static void setup(Project project) {
		MinecraftDepsExtension mc = new MinecraftDepsExtension(project.getDependencies());
		project.getDependencies().getExtensions().add("minecraft", mc);
	}
}
