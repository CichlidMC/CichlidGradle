package fish.cichlidmc.cichlid_gradle.cache.storage;

import java.nio.file.Path;

public final class TransformedClassStorage {
	private final Path root;

	public TransformedClassStorage(Path root) {
		this.root = root;
	}

	public Path get(String defHash, String bytecodeHash) {
		return this.root.resolve(defHash).resolve(bytecodeHash + ".class");
	}
}
