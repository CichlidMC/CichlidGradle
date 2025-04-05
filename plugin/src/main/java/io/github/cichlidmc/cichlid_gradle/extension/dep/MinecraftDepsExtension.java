package io.github.cichlidmc.cichlid_gradle.extension.dep;

import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;

public class MinecraftDepsExtension {
	// can't use managed properties - https://github.com/gradle/gradle/issues/18213
	private final DependencyFactory factory;

    private MinecraftDepsExtension(DependencyFactory factory) {
        this.factory = factory;
    }

    public ExternalModuleDependency client(String version) {
		return this.ofDist(Distribution.CLIENT, version);
	}

	public ExternalModuleDependency server(String version) {
		return this.ofDist(Distribution.SERVER, version);
	}

	public ExternalModuleDependency merged(String version) {
		return this.ofDist(Distribution.MERGED, version);
	}

	public ExternalModuleDependency of(Action<MinecraftSpec> action) {
		MinecraftSpecImpl spec = new MinecraftSpecImpl();
		action.execute(spec);
		return spec.createDependencyOrThrow(this.factory);
	}

	private ExternalModuleDependency ofDist(Distribution dist, String version) {
		return this.of(spec -> spec.distribution(dist).version(version));
	}

	public static void setup(Project project) {
		MinecraftDepsExtension mc = new MinecraftDepsExtension(project.getDependencyFactory());
		project.getDependencies().getExtensions().add("minecraft", mc);
	}
}
