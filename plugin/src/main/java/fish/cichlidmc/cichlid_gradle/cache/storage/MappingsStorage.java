package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.util.Distribution;

import java.nio.file.Path;

public class MappingsStorage {
	public final Path root;

	MappingsStorage(Path root) {
		this.root = root;
	}

	public Path path(Distribution distribution) {
		return this.root.resolve(distribution + ".txt");
	}

	public Path log(Distribution distribution) {
		return this.root.resolve(distribution + "-log.txt");
	}
}
