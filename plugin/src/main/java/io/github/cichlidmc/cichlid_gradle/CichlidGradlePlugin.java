package io.github.cichlidmc.cichlid_gradle;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import io.github.cichlidmc.cichlid_gradle.extension.dep.CichlidDepsExtension;
import io.github.cichlidmc.cichlid_gradle.extension.dep.MinecraftDepsExtension;
import io.github.cichlidmc.cichlid_gradle.extension.repo.CichlidReposExtension;
import io.github.cichlidmc.cichlid_gradle.extension.repo.MinecraftReposExtension;
import io.github.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import io.github.cichlidmc.cichlid_gradle.transform.TransformedAttribute;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyScopeConfiguration;

import java.util.Objects;

public abstract class CichlidGradlePlugin implements Plugin<Project> {
	public static final String NAME = "CichlidGradle";
	public static final String VERSION = "1.0-SNAPSHOT";

	public static final String CICHLID_CONFIGURATION = "cichlid";

	@Override
	public void apply(Project project) {
		CichlidExtension.setup(project);
		CichlidReposExtension.setup(project);
		MinecraftReposExtension.setup(project);
		CichlidDepsExtension.setup(project);
		MinecraftDepsExtension.setup(project);
		RunTaskGeneration.setup(project);
		TransformedAttribute.setup(project);

		ConfigurationContainer configurations = project.getConfigurations();
		setupConfigurations(configurations);

		// listen for all minecraft dependencies to ensure they're downloaded
		configurations.configureEach(
				configuration -> configuration.getDependencies().configureEach(dep -> {
					if (isMinecraftDependency(dep)) {
						String version = Objects.requireNonNull(dep.getVersion());
						CichlidCache cache = CichlidCache.get(project);
						cache.ensureVersionIsCached(version);
					}
				})
		);
	}

	private static void setupConfigurations(ConfigurationContainer configurations) {
		// configurations for Cichlid.
		// you need one configuration for declaring dependencies and another extending it for resolution
		NamedDomainObjectProvider<DependencyScopeConfiguration> cichlidRuntime = configurations.dependencyScope("cichlidRuntime");
		// resolvable configuration is non-transitive so it only includes Cichlid itself
		configurations.resolvable(CICHLID_CONFIGURATION, resolvable -> resolvable.extendsFrom(cichlidRuntime.get()).setTransitive(false));

		// add Cichlid and its dependencies to the runtime classpath
		configurations.named("runtimeClasspath", runtime -> runtime.extendsFrom(cichlidRuntime.get()));

		// compile classpath must be transformed
		configurations.named("compileClasspath", compile -> compile.getAttributes().attribute(TransformedAttribute.INSTANCE, true));
	}

	private static boolean isMinecraftDependency(Dependency dep) {
		return "net.minecraft".equals(dep.getGroup())
				&& CichlidCache.MINECRAFT_MODULES.contains(dep.getName())
				&& dep.getVersion() != null;
	}
}
