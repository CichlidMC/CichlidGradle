package fish.cichlidmc.cichlid_gradle.extension.dep;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;

public class CichlidDepsExtension {
	// can't use managed properties - https://github.com/gradle/gradle/issues/18213
	private final DependencyFactory factory;

    private CichlidDepsExtension(DependencyFactory factory) {
        this.factory = factory;
    }

    public ExternalModuleDependency runtime(String version) {
		return this.factory.create("fish.cichlidmc", "cichlid", version);
	}

	public ExternalModuleDependency api(String version) {
		ExternalModuleDependency dep = this.runtime(version);
		dep.capabilities(handler -> handler.requireCapability("fish.cichlidmc:cichlid-mod-api"));
		return dep;
	}

	public ExternalModuleDependency pluginApi(String version) {
		ExternalModuleDependency dep = this.runtime(version);
		dep.capabilities(handler -> handler.requireCapability("fish.cichlidmc:cichlid-plugin-api"));
		return dep;
	}

	public Dependency cichlibs(String version) {
		return this.factory.create("fish.cichlidmc:cichlibs:" + version);
	}

	public static void setup(Project project) {
		CichlidDepsExtension mc = new CichlidDepsExtension(project.getDependencyFactory());
		project.getDependencies().getExtensions().add("cichlid", mc);
	}
}
