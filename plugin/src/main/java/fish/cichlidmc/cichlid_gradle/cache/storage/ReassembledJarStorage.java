package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.cache.Artifact;
import fish.cichlidmc.cichlid_gradle.util.Distribution;

import java.nio.file.Path;

public final class ReassembledJarStorage {
	private final Path root;

	public ReassembledJarStorage(Path root) {
		this.root = root;
	}

	public Path get(String version, String defHash, Distribution dist, Artifact artifact) {
		return switch (artifact) {
			case JAR -> this.binary(version, defHash, dist);
			case SOURCES -> this.sources(version, defHash, dist);
			case POM -> throw new IllegalArgumentException("poms are not jars");
		};
	}

	public Path get(String version, String defHash, Distribution dist, boolean sources) {
		return sources ? this.sources(version, defHash, dist) : this.binary(version, defHash, dist);
	}

	public Path binary(String version, String defHash, Distribution dist) {
		return this.path(version, defHash, dist, "binary");
	}

	public Path sources(String version, String defHash, Distribution dist) {
		return this.path(version, defHash, dist, "sources");
	}

	private Path path(String version, String defHash, Distribution dist, String name) {
		return this.root.resolve(version).resolve(dist.name).resolve(defHash).resolve(name + ".jar");
	}
}
