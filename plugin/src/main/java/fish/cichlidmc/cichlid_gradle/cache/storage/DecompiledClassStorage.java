package fish.cichlidmc.cichlid_gradle.cache.storage;

import java.nio.file.Path;

public final class DecompiledClassStorage {
	private final Path root;

	public DecompiledClassStorage(Path root) {
		this.root = root;
	}

	/**
	 * Note that for classes containing inner classes, the hash includes the inner class bytecode, hashed alphabetically.
	 * The inner classes are not cached individually.
	 */
	public Path get(String bytecodeHash) {
		return this.root.resolve(bytecodeHash + ".java");
	}

	public Path linemap(String bytecodeHash) {
		return this.root.resolve(bytecodeHash + ".linemap");
	}
}
