package fish.cichlidmc.cichlid_gradle.extension.def;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.FileUtils;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public final class MinecraftDefinitionImpl implements MinecraftDefinition {
	private final String name;
	private final NamedDomainObjectProvider<DependencyScopeConfiguration> depTransformers;
	private final NamedDomainObjectProvider<ResolvableConfiguration> resolvableTransformers;

	private final Property<String> version;
	private final Property<Distribution> distribution;

	private final Provider<ExternalModuleDependency> dependency;

	public MinecraftDefinitionImpl(String name, Project project) {
		this.name = name;

		ConfigurationContainer configurations = project.getConfigurations();
		this.depTransformers = configurations.dependencyScope(name + "Transformer");
		this.resolvableTransformers = configurations.resolvable(
				name + "Transformers", resolvable -> resolvable.extendsFrom(this.depTransformers.get())
		);

		ObjectFactory objects = project.getObjects();

		this.version = objects.property(String.class);
		this.version.finalizeValueOnRead();

		this.distribution = objects.property(Distribution.class).convention(Distribution.MERGED);
		this.distribution.finalizeValueOnRead();

		DependencyFactory depFactory = project.getDependencyFactory();
		this.dependency = project.getProviders().provider(() -> {
			String hash = FileUtils.sha1All(this.resolvableTransformers().getIncoming().getFiles().getFiles());
			return depFactory.create("net.minecraft", "minecraft", name + '_' + hash.substring(0, 10));
		});
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Property<String> getVersion() {
		return this.version;
	}

	@Override
	public Property<Distribution> getDistribution() {
		return this.distribution;
	}

	@Override
	public DependencyScopeConfiguration getTransformer() {
		return this.depTransformers.get();
	}

	@Override
	public Provider<ExternalModuleDependency> getDependency() {
		return this.dependency;
	}

	public String getVersionOrThrow() {
		if (this.version.isPresent()) {
			return this.version.get();
		}

		throw new InvalidUserDataException("Minecraft definition '" + this.name + "' does not have a version specified");
	}

	public ResolvableConfiguration resolvableTransformers() {
		return this.resolvableTransformers.get();
	}
}
