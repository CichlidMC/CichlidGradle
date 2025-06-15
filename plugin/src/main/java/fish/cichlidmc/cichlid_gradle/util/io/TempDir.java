package fish.cichlidmc.cichlid_gradle.util.io;

import fish.cichlidmc.cichlid_gradle.CichlidGradlePlugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record TempDir(Path path) implements AutoCloseable {
	public TempDir() throws IOException {
		this(Files.createTempDirectory(CichlidGradlePlugin.NAME));
	}

	@Override
	public void close() throws IOException {
		if (!Files.exists(this.path)) {
			throw new FileNotFoundException("Temp dir already deleted: " + this.path);
		}

		FileUtils.deleteRecursively(this.path);
		Files.delete(this.path);
	}
}
