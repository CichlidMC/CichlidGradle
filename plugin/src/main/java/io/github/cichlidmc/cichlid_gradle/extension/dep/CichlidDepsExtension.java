package io.github.cichlidmc.cichlid_gradle.extension.dep;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class CichlidDepsExtension {
	// can't use managed properties - https://github.com/gradle/gradle/issues/18213
	private final DependencyHandler deps;

    private CichlidDepsExtension(DependencyHandler deps) {
        this.deps = deps;
    }

    public Dependency loader(String version) {
		return this.deps.create("io.github.cichlidmc:cichlid:" + version);
	}

	public Dependency loaderApi(String version) {
		return this.deps.create("io.github.cichlidmc:cichlid:" + version + ":api");
	}

	public Dependency devPlugin(String version) {
		return this.deps.create("io.github.cichlidmc:dev-plugin:" + version);
	}

	public static void setup(Project project) {
		CichlidDepsExtension mc = new CichlidDepsExtension(project.getDependencies());
		project.getDependencies().getExtensions().add("cichlid", mc);
	}
}
