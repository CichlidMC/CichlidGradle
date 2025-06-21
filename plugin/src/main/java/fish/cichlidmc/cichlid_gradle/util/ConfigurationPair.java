package fish.cichlidmc.cichlid_gradle.util;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.ResolvableDependencies;

public record ConfigurationPair(NamedDomainObjectProvider<DependencyScopeConfiguration> depScope, NamedDomainObjectProvider<ResolvableConfiguration> resolvable) {
	public ResolvableDependencies resolve() {
		return this.resolvable.get().getIncoming();
	}

	public static ConfigurationPair of(String name, ConfigurationContainer configurations) {
		NamedDomainObjectProvider<DependencyScopeConfiguration> depScope = configurations.dependencyScope(name);
		NamedDomainObjectProvider<ResolvableConfiguration> resolvable = configurations.resolvable(
				name + "$transformerMods", configuration -> configuration.extendsFrom(depScope.get())
		);
		return new ConfigurationPair(depScope, resolvable);
	}
}
