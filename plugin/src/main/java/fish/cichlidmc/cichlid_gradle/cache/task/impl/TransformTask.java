package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.TransformedClassStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.sushi.api.TransformerManager;
import fish.cichlidmc.sushi.api.util.Id;
import fish.cichlidmc.tinyjson.value.composite.JsonObject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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

					Path path = output.get(TransformTask.this.env.transformers.hash(), hash);
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

	private TransformerManager prepareTransformers(Stats stats) {
		TransformerManager.Builder builder = TransformerManager.builder();

		builder.parseAndRegister(
				new Id("cichlid_gradle", "test_transformer"),
				new JsonObject()
						.put("target", "net.minecraft.client.main.Main")
						.put(
								"transforms",
								new JsonObject()
										.put("type", "add_interface")
										.put("interface", TestInterfacePleaseIgnore.class.getName())
						)
		).ifPresent(error -> {
			throw new RuntimeException(error);
		});

		stats.transformers++;

		return builder.build();
	}

	public interface TestInterfacePleaseIgnore {
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
