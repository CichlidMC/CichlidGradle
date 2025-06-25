package fish.cichlidmc.cichlid_gradle.extension.def;

import fish.cichlidmc.cichlid_gradle.run.RunConfiguration;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public interface MinecraftDefinition extends Named {
	// specially named properties for DSL
	Property<String> getVersion();
	Property<Distribution> getDistribution();
	Provider<ExternalModuleDependency> getDependency();
	Transformers getTransformers();
	NamedDomainObjectContainer<? extends RunConfiguration> getRuns();

	interface Transformers {
		DependencyScopeConfiguration getMod();
		DependencyScopeConfiguration namespaced(String namespace);
	}

	static void setup(Project project) {
		project.getExtensions().add("minecraft", project.getObjects().domainObjectContainer(
				MinecraftDefinition.class,
				name -> new MinecraftDefinitionImpl(name, project)
		));
	}

	@SuppressWarnings("unchecked")
	static NamedDomainObjectContainer<MinecraftDefinition> getExtension(Project project) {
		return (NamedDomainObjectContainer<MinecraftDefinition>) project.getExtensions().getByName("minecraft");
	}
}
