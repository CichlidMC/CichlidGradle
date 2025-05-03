package fish.cichlidmc.cichlid_gradle;

import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.cache.mcmaven.McMavenConnectorFactory;
import fish.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinition;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinitionImpl;
import fish.cichlidmc.cichlid_gradle.extension.dep.CichlidDepsExtension;
import fish.cichlidmc.cichlid_gradle.extension.repo.CichlidReposExtension;
import fish.cichlidmc.cichlid_gradle.extension.repo.MinecraftReposExtension;
import fish.cichlidmc.cichlid_gradle.run.RunTaskGeneration;
import fish.cichlidmc.cichlid_gradle.util.Utils;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.internal.artifacts.cache.ArtifactResolutionControl;
import org.gradle.api.internal.artifacts.cache.ModuleResolutionControl;
import org.gradle.api.internal.artifacts.configurations.ResolutionStrategyInternal;
import org.gradle.api.internal.artifacts.ivyservice.resolutionstrategy.DefaultCachePolicy;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

		// minecraft dependencies are set as changing so the current transformers are always applied.
		// we need to set its cache duration to 0, but gradle API only allows you to do that per-configuration.
		// it's possible with some internals though.
		configurations.configureEach(CichlidGradlePlugin::disableMinecraftCaching);

		// if we try to resolve transformers for the first time in the mcmaven, an error
		// is thrown because the current thread is already resolving a configuration,
		// so we need to resolve them right before they're actually needed.

		NamedDomainObjectContainer<MinecraftDefinition> defs = MinecraftDefinition.getExtension(project);

		// for each configuration, right before resolution:
		// - check if any dependencies are Minecraft
		// - if so, find the corresponding definition
		// - if it exists, resolve its transformers
		configurations.configureEach(configuration -> configuration.getIncoming().beforeResolve(resolvable -> {
			for (Dependency dep : resolvable.getDependencies()) {
				if (!isMinecraft(dep))
					continue;

				String defName = dep.getVersion();
				if (defName == null)
					continue;

				MinecraftDefinitionImpl def = (MinecraftDefinitionImpl) defs.findByName(defName);
				if (def == null)
					continue;

				// call iterator to make sure they're actually resolved
				def.resolvableTransformers().getIncoming().getFiles().iterator();
			}
		}));
	}

	private static void disableMinecraftCaching(Configuration configuration) {
		try {
			ResolutionStrategyInternal strategy = (ResolutionStrategyInternal) configuration.getResolutionStrategy();
			DefaultCachePolicy policy = (DefaultCachePolicy) strategy.getCachePolicy();

			Field moduleCacheRulesField = DefaultCachePolicy.class.getDeclaredField("moduleCacheRules");
			moduleCacheRulesField.setAccessible(true);
			Field artifactCacheRulesField = DefaultCachePolicy.class.getDeclaredField("artifactCacheRules");
			artifactCacheRulesField.setAccessible(true);

			List<Action<? super ModuleResolutionControl>> moduleCacheRules = Utils.get(moduleCacheRulesField, policy);
			List<Action<? super ArtifactResolutionControl>> artifactCacheRules = Utils.get(artifactCacheRulesField, policy);

			// add to the ends of the lists to get final say.
			// gradle conveniently adds to the front.

			moduleCacheRules.add(control -> {
				if (isMinecraft(control.getRequest().getModule())) {
					control.cacheFor(0, TimeUnit.SECONDS);
				}
			});
			artifactCacheRules.add(control -> {
				if (isMinecraft(control.getRequest().getId().getComponentIdentifier().getModuleIdentifier())) {
					control.cacheFor(0, TimeUnit.SECONDS);
				}
			});
		} catch (Throwable t) {
			throw new RuntimeException("Error accessing Gradle internals! You probably need to update Gradle, CichlidGradle, or both", t);
		}
	}

	private static boolean isMinecraft(ModuleIdentifier id) {
		return isMinecraft(id.getGroup(), id.getName());
	}

	private static boolean isMinecraft(Dependency dep) {
		return isMinecraft(dep.getGroup(), dep.getName());
	}

	private static boolean isMinecraft(@Nullable String group, @Nullable String module) {
		return CichlidCache.MINECRAFT_GROUP.equals(group) && CichlidCache.MINECRAFT_MODULE.equals(module);
	}
}
