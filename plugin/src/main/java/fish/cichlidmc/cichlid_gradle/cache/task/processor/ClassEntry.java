package fish.cichlidmc.cichlid_gradle.cache.task.processor;

import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @param fileName the fully qualified class name + extension, ex. {@code net/minecraft/ClassName$Inner.class}
 */
public record ClassEntry(String fileName, Content content) {
	public ClassEntry(String fileName, byte[] bytes) {
		this(fileName, new ByteContent(bytes));
	}

	public ClassEntry(String fileName, Path file) {
		this(fileName, new FileContent(file));
	}

	public ClassEntry withContent(Content content) {
		return new ClassEntry(this.fileName, content);
	}

	public ClassEntry withContent(Path file) {
		return this.withContent(new FileContent(file));
	}

	public ClassEntry withContent(byte[] bytes) {
		return this.withContent(new ByteContent(bytes));
	}

	@FunctionalInterface
	public interface Content {
		byte[] bytes() throws IOException;

		default void write(Path output) throws IOException {
			FileUtils.ensureCreated(output);
			Files.write(output, this.bytes());
		}
	}

	public record ByteContent(byte[] bytes) implements Content {
	}

	public record FileContent(Path path) implements Content {
		@Override
		public byte[] bytes() throws IOException {
			return Files.readAllBytes(this.path);
		}

		@Override
		public void write(Path output) throws IOException {
			FileUtils.copy(this.path, output);
		}
	}
}
