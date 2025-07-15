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
		return this.get(bytecodeHash, "java");
	}

	public Path linemap(String bytecodeHash) {
		return this.get(bytecodeHash, "linemap");
	}

	private Path get(String bytecodeHash, String extension) {
		String start = bytecodeHash.substring(0, 2);
		return this.root.resolve(start).resolve(bytecodeHash + '.' + extension);
	}
}
