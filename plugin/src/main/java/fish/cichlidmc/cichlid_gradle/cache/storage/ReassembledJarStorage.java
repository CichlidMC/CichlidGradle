package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.cache.Artifact;
import fish.cichlidmc.cichlid_gradle.util.Distribution;

import java.nio.file.Path;

public final class ReassembledJarStorage {
	private final Path root;

	public ReassembledJarStorage(Path root) {
		this.root = root;
	}

	public Path get(String version, String transformersHash, Distribution dist, Artifact artifact) {
		return switch (artifact) {
			case JAR -> this.binary(version, transformersHash, dist);
			case SOURCES -> this.sources(version, transformersHash, dist);
			case POM -> throw new IllegalArgumentException("poms are not jars");
		};
	}

	public Path get(String version, String transformersHash, Distribution dist, boolean sources) {
		return sources ? this.sources(version, transformersHash, dist) : this.binary(version, transformersHash, dist);
	}

	public Path binary(String version, String transformersHash, Distribution dist) {
		return this.path(version, transformersHash, dist, "binary");
	}

	public Path sources(String version, String transformersHash, Distribution dist) {
		return this.path(version, transformersHash, dist, "sources");
	}

	private Path path(String version, String transformersHash, Distribution dist, String name) {
		return this.root.resolve(version).resolve(dist.name).resolve(transformersHash).resolve(name + ".jar");
	}
}
