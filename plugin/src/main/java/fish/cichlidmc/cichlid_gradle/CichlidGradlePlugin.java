package fish.cichlidmc.cichlid_gradle;

import fish.cichlidmc.cichlid_gradle.cache.mcmaven.McMavenConnectorFactory;
import fish.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinition;
import fish.cichlidmc.cichlid_gradle.extension.dep.CichlidDepsExtension;
import fish.cichlidmc.cichlid_gradle.extension.repo.CichlidReposExtension;
import fish.cichlidmc.cichlid_gradle.extension.repo.MinecraftReposExtension;
import fish.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;

import javax.inject.Inject;

public class CichlidGradlePlugin implements Plugin<Project> {
	public static final String NAME = "CichlidGradle";
	public static final String VERSION = "1.0-SNAPSHOT";

	public static final String CICHLID_CONFIGURATION = "cichlid";

	private final RepositoryTransportFactory repositoryTransportFactory;

	@Inject
	public CichlidGradlePlugin(RepositoryTransportFactory repositoryTransportFactory) {
		this.repositoryTransportFactory = repositoryTransportFactory;
	}

	@Override
	public void apply(Project project) {
		CichlidExtension.setup(project);
		CichlidReposExtension.setup(project);
		MinecraftReposExtension.setup(project);
		CichlidDepsExtension.setup(project);
		RunTaskGeneration.setup(project);
		MinecraftDefinition.setup(project);

		setupConfigurations(project);

		McMavenConnectorFactory.inject(this.repositoryTransportFactory, project);
	}

	private static void setupConfigurations(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();

		// configurations for Cichlid.
		// you need one configuration for declaring dependencies and another extending it for resolution
		NamedDomainObjectProvider<DependencyScopeConfiguration> cichlidRuntime = configurations.dependencyScope("cichlidRuntime");
		// resolvable configuration is non-transitive so it only includes Cichlid itself
		configurations.resolvable(CICHLID_CONFIGURATION, resolvable -> resolvable.extendsFrom(cichlidRuntime.get()).setTransitive(false));

		// add Cichlid and its dependencies to the runtime classpath
		configurations.named("runtimeClasspath", runtime -> runtime.extendsFrom(cichlidRuntime.get()));
	}
}
