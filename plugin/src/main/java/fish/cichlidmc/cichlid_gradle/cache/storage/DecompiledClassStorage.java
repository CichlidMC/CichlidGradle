package fish.cichlidmc.cichlid_gradle.cache.storage;

import java.nio.file.Path;

public final class DecompiledClassStorage {
	private final Path root;

	public DecompiledClassStorage(Path root) {
		this.root = root;
	}

	public Path get(String bytecodeHash) {
		return this.root.resolve(bytecodeHash + ".java");
	}
}
