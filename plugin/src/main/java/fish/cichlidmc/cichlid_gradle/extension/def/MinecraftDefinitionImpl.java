package fish.cichlidmc.cichlid_gradle.extension.def;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
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

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public final class MinecraftDefinitionImpl implements MinecraftDefinition {
	private final String name;
	private final NamedDomainObjectProvider<DependencyScopeConfiguration> depTransformers;
	private final NamedDomainObjectProvider<ResolvableConfiguration> resolvableTransformers;

	private final Property<String> version;
	private final Property<Distribution> distribution;

	private final Provider<ExternalModuleDependency> dependency;

	public MinecraftDefinitionImpl(String name, Project project) {
		if (name.contains("$")) {
			throw new InvalidUserDataException("Minecraft definition names may not contain '$'");
		}

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
		this.dependency = project.getProviders().provider(
				() -> depFactory.create("net.minecraft", "minecraft", name + '$' + this.hashTransformers())
		);
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

	public Distribution dist() {
		return this.distribution.get();
	}

	public ResolvableConfiguration resolvableTransformers() {
		return this.resolvableTransformers.get();
	}

	private String hashTransformers() throws IOException {
		Set<File> files = this.resolvableTransformers().getIncoming().getFiles().getFiles();
		Set<File> sorted = new TreeSet<>(FileUtils.FILE_COMPARATOR_BY_NAME);
		sorted.addAll(files);

		return Encoding.BASE_FUNNY.encode(HashAlgorithm.SHA256.hashFiles(sorted));
	}
}
