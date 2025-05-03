package fish.cichlidmc.cichlid_gradle.extension.def;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public interface MinecraftDefinition extends Named {
	// specially named properties for DSL
	Property<String> getVersion();
	Property<Distribution> getDistribution();
	DependencyScopeConfiguration getTransformer();
	ExternalModuleDependency getDependency();

	static void setup(Project project) {
		ObjectFactory objects = project.getObjects();
		ConfigurationContainer configurations = project.getConfigurations();
		DependencyFactory depFactory = project.getDependencyFactory();

		NamedDomainObjectContainer<MinecraftDefinition> container = objects.domainObjectContainer(
				MinecraftDefinition.class,
				name -> new MinecraftDefinitionImpl(name, configurations, objects, depFactory)
		);

		project.getExtensions().add("minecraft", container);
	}

	@SuppressWarnings("unchecked")
	static NamedDomainObjectContainer<MinecraftDefinition> getExtension(Project project) {
		return (NamedDomainObjectContainer<MinecraftDefinition>) project.getExtensions().getByName("minecraft");
	}
}
