package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.mcmaven.JarProcessor;
import fish.cichlidmc.cichlid_gradle.cache.storage.DecompiledClassStorage;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public record CacheResultSaver(DecompiledClassStorage storage, Map<String, JarProcessor.ClassGroup> groups) implements IResultSaver {
	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content, int[] mapping) {
		JarProcessor.ClassGroup group = this.groups.get(qualifiedName);
		if (group == null) {
			throw new IllegalStateException("Class " + qualifiedName + " has no ClassGroup");
		}

		try {
			String hash = group.hash();
			Path output = this.storage.get(hash);
			if (Files.exists(output))
				return;

			FileUtils.ensureCreated(output);
			Files.writeString(output, content);

			if (mapping != null) {
				Path linemap = this.storage.linemap(hash);
				FileUtils.ensureCreated(linemap);
				String string = Arrays.stream(mapping)
						.mapToObj(String::valueOf)
						.collect(Collectors.joining("\n"));

				Files.writeString(linemap, string);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to save class " + qualifiedName, e);
		}
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveFolder(String path) {
	}

	@Override
	public void copyFile(String source, String path, String entryName) {
	}

	@Override
	public void createArchive(String path, String archiveName, Manifest manifest) {
	}

	@Override
	public void saveDirEntry(String path, String archiveName, String entryName) {
	}

	@Override
	public void copyEntry(String source, String path, String archiveName, String entry) {
	}

	@Override
	public void closeArchive(String path, String archiveName) {
	}
}
