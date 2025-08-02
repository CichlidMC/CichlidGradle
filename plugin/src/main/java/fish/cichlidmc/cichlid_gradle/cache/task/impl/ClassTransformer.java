package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.cache.task.processor.ClassEntry;
import fish.cichlidmc.cichlid_gradle.cache.task.processor.ClassGroup;
import fish.cichlidmc.cichlid_gradle.cache.task.processor.JarProcessor;
import fish.cichlidmc.cichlid_gradle.extension.def.TransformersImpl;
import fish.cichlidmc.cichlid_gradle.util.ConfigurationPair;
import fish.cichlidmc.cichlid_gradle.util.Utils;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public record ClassTransformer(TransformerManager manager) implements JarProcessor.ClassProcessor {
	@Override
	public ClassGroup apply(ClassGroup group) throws IOException {
		return new ClassGroup(this.transform(group.main()), Utils.map(group.inner(), this::transform));
	}

	private ClassEntry transform(ClassEntry entry) throws IOException {
		if (entry.fileName().endsWith("package-info.class"))
			return entry;

		byte[] bytes = entry.content().bytes();
		ClassReader reader = new ClassReader(bytes);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);

		if (this.manager.transform(node, reader)) {
			ClassWriter writer = new ClassWriter(reader, 0);
			node.accept(writer);
			return entry.withContent(writer.toByteArray());
		}

		return entry;
	}

	public static ClassTransformer create(CacheTaskEnvironment env) throws IOException {
		TransformerManager manager = prepareTransformers(env.transformers);
		return new ClassTransformer(manager);
	}

	private static TransformerManager prepareTransformers(TransformersImpl transformers) throws IOException {
		TransformerManager.Builder builder = TransformerManager.builder();

		for (File file : transformers.mods.resolve().getFiles()) {
			Path path = file.toPath();
			if (Files.isDirectory(path)) {
				readTransformersFromMod(path, builder);
				continue;
			}

			if (path.getFileName().toString().endsWith(".cld")) {
				try (FileSystem fs = FileSystems.newFileSystem(path)) {
					Path root = FileUtils.getSingleRoot(fs);
					readTransformersFromMod(root, builder);
				}

				continue;
			}

			throw new IllegalArgumentException("Transformer-providing mod is not in a valid format: " + file);
		}

		for (Map.Entry<String, ConfigurationPair> entry : transformers.namespaced.entrySet()) {
			String namespace = entry.getKey();
			for (File file : entry.getValue().resolve().getFiles()) {
				readTransformersFromTree(namespace, file.toPath(), builder);
			}
		}

		return builder.build();
	}

	private static void readTransformersFromMod(Path root, TransformerManager.Builder builder) throws IOException {
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
			readTransformersFromTree(id, transformers, builder);
		} catch (JsonException e) {
			throw new IllegalArgumentException("Mod root contains malformed metadata: " + root, e);
		}
	}

	private static void readTransformersFromTree(String namespace, Path root, TransformerManager.Builder builder) throws IOException {
		FileUtils.walkFiles(root, file -> {
			String relative = FileUtils.safeRelativize(root, file);
			if (!relative.endsWith(".sushi"))
				return;

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
			} catch (JsonException e) {
				throw new IllegalArgumentException("Transformer " + id + " is not valid JSON", e);
			}
		});
	}
}
