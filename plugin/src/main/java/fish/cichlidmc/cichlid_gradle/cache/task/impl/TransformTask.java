package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.TransformedClassStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.util.ConfigurationPair;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.sushi.api.TransformerManager;
import fish.cichlidmc.sushi.api.util.Id;
import fish.cichlidmc.tinyjson.JsonException;
import fish.cichlidmc.tinyjson.TinyJson;
import fish.cichlidmc.tinyjson.value.JsonValue;
import fish.cichlidmc.tinyjson.value.composite.JsonObject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class TransformTask extends CacheTask {
	protected TransformTask(CacheTaskEnvironment env) {
		super("Transform", env);
	}

	@Override
	protected String run() throws IOException {
		Path jar = this.env.cache.getVersion(this.env.version.id).jars.get(this.env.dist);
		FileUtils.assertExists(jar);

		TransformedClassStorage output = this.env.cache.transformedClasses;
		Stats stats = new Stats();
		TransformerManager manager = this.prepareTransformers(stats);

		try (FileSystem fs = FileSystems.newFileSystem(jar)) {
			Path root = FileUtils.getSingleRoot(fs);
			Files.walkFileTree(root, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String name = file.getFileName().toString();
					if (!name.endsWith(".class") || name.endsWith("package-info.class"))
						return FileVisitResult.CONTINUE;

					stats.totalClasses++;

					byte[] bytes = Files.readAllBytes(file);
					String hash = Encoding.BASE_FUNNY.encode(HashAlgorithm.SHA256.hash(bytes));

					Path path = output.get(TransformTask.this.env.hash, hash);
					if (Files.exists(path)) {
						return FileVisitResult.CONTINUE;
					}

					stats.transformedClasses++;

					ClassReader reader = new ClassReader(bytes);
					ClassNode node = new ClassNode();
					reader.accept(node, 0);

					manager.transform(node, reader);

					ClassWriter writer = new ClassWriter(reader, 0);
					node.accept(writer);

					FileUtils.ensureCreated(path);
					Files.write(path, writer.toByteArray());

					return FileVisitResult.CONTINUE;
				}
			});
		}

		return stats.toString();
	}

	private TransformerManager prepareTransformers(Stats stats) throws IOException {
		TransformerManager.Builder builder = TransformerManager.builder();

		for (File file : this.env.transformers.mods.resolve().getFiles()) {
			Path path = file.toPath();
			if (Files.isDirectory(path)) {
				readTransformersFromMod(path, builder, stats);
				continue;
			}

			if (path.getFileName().toString().endsWith(".cld")) {
				try (FileSystem fs = FileSystems.newFileSystem(path)) {
					Path root = FileUtils.getSingleRoot(fs);
					readTransformersFromMod(root, builder, stats);
				}

				continue;
			}

			throw new IllegalArgumentException("Transformer-providing mod is not in a valid format: " + file);
		}
		
		for (Map.Entry<String, ConfigurationPair> entry : this.env.transformers.namespaced.entrySet()) {
			String namespace = entry.getKey();
			for (File file : entry.getValue().resolve().getFiles()) {
				readTransformersFromTree(namespace, file.toPath(), builder, stats);
			}
		}

		return builder.build();
	}

	private static void readTransformersFromMod(Path root, TransformerManager.Builder builder, Stats stats) throws IOException {
		Path transformers = root.resolve("transformers");
		if (!Files.exists(transformers))
			return;

		Path metadata = root.resolve("cichlid.mod.json");
		if (!Files.exists(metadata)) {
			throw new IllegalArgumentException("Mod root does not contain metadata: " + root);
		}

		try {
			JsonObject json = TinyJson.parse(metadata).asObject();
			String id = json.get("id").asString().value();
			readTransformersFromTree(id, transformers, builder, stats);
		} catch (JsonException e) {
			throw new IllegalArgumentException("Mod root contains malformed metadata: " + root, e);
		}
	}

	private static void readTransformersFromTree(String namespace, Path root, TransformerManager.Builder builder, Stats stats) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String relative = root.relativize(file).toString();
				if (!relative.endsWith(".sushi")) {
					return FileVisitResult.CONTINUE;
				}

				String path = relative.substring(0, relative.length() - ".sushi".length());
				if (!Id.isValidPath(path)) {
					throw new IllegalArgumentException("Mod '" + namespace + "' has a transformer with an invalid ID: " + path);
				}

				Id id = new Id(namespace, path);

				try {
					JsonValue json = TinyJson.parse(file);


					builder.parseAndRegister(id, json).ifPresent(error -> {
						throw new IllegalArgumentException("Transformer " + id + " could not be registered: " + error);
					});

					stats.transformers++;
					return FileVisitResult.CONTINUE;
				} catch (JsonException e) {
					throw new IllegalArgumentException("Transformer " + id + " is not valid JSON", e);
				}
			}
		});
	}

	private static final class Stats {
		private int transformers;
		private int totalClasses;
		private int transformedClasses;

		@Override
		public String toString() {
			return String.format(
					"Transformed %s class(es) with %s transformer(s), %s of which were uncached.",
					this.totalClasses, this.transformers, this.transformedClasses
			);
		}
	}
}
