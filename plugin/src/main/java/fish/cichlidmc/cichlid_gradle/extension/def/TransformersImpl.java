package fish.cichlidmc.cichlid_gradle.extension.def;

import fish.cichlidmc.cichlid_gradle.util.ConfigurationPair;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.sushi.api.util.Id;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyScopeConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TransformersImpl implements MinecraftDefinition.Transformers {
	public final ConfigurationPair mods;
	public final Map<String, ConfigurationPair> namespaced;
	private final Function<String, ConfigurationPair> factory;

	public TransformersImpl(ConfigurationPair mods, Function<String, ConfigurationPair> factory) {
		this.mods = mods;
		this.namespaced = new HashMap<>();
		this.factory = factory;
	}

	@Override
	public DependencyScopeConfiguration getMod() {
		return this.mods.depScope().get();
	}

	@Override
	public DependencyScopeConfiguration namespaced(String namespace) {
		if (!Id.isValidNamespace(namespace)) {
			throw new InvalidUserDataException("Invalid namespace: " + namespace);
		}

		return this.namespaced.computeIfAbsent(namespace, this.factory).depScope().get();
	}

	public Set<File> collectFiles() throws IOException {
		Set<File> files = new TreeSet<>(FileUtils.FILE_COMPARATOR);

		collect(this.mods, files::add);
		for (ConfigurationPair pair : this.namespaced.values()) {
			collect(pair, files::add);
		}

		return files;
	}

	private static void collect(ConfigurationPair pair, Consumer<File> output) throws IOException {
		for (File file : pair.resolve().getFiles()) {
			if (!file.exists())
				continue;

			if (!file.isDirectory()) {
				output.accept(file);
				continue;
			}

			Path root = file.toPath();
			Files.walkFileTree(root, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					output.accept(file.toFile());
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	public static TransformersImpl of(String name, ConfigurationContainer configurations) {
		return new TransformersImpl(
				ConfigurationPair.of(name + "$transformerMod", configurations),
				namespace -> ConfigurationPair.of(name + '$' + namespace + "$namespacedTransformer", configurations)
		);
	}
}
