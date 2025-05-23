package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.util.Distribution;

import java.nio.file.Path;

public final class PomTemplateStorage {
	private final Path root;

	public PomTemplateStorage(Path root) {
		this.root = root;
	}

	public Path get(String version, Distribution dist) {
		return this.root.resolve(version).resolve(dist.name + ".xml");
	}
}
