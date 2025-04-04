package io.github.cichlidmc.cichlid_gradle.extension.dep;

import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.Action;
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
		return this.ofDist(Distribution.CLIENT, version);
	}

	public Dependency server(String version) {
		return this.ofDist(Distribution.SERVER, version);
	}

	public Dependency merged(String version) {
		return this.ofDist(Distribution.MERGED, version);
	}

	public Dependency of(Action<MinecraftSpec> action) {
		MinecraftSpecImpl spec = new MinecraftSpecImpl();
		action.execute(spec);
		return this.deps.create(spec.toDependencyString());
	}

	private Dependency ofDist(Distribution dist, String version) {
		return this.of(spec -> spec.distribution(dist).version(version));
	}

	public static void setup(Project project) {
		MinecraftDepsExtension mc = new MinecraftDepsExtension(project.getDependencies());
		project.getDependencies().getExtensions().add("minecraft", mc);
	}
}
