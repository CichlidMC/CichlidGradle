package io.github.cichlidmc.cichlid_gradle.cache.storage;

import java.nio.file.Path;

public class NativesStorage {
	public final Path root;

	NativesStorage(Path root) {
		this.root = root;
	}

	public Path folder(String version) {
		return this.root.resolve(version);
	}
}
