package fish.cichlidmc.cichlid_gradle.util.io;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class LockFile {
	public static final String SUFFIX = ".lock";

	private static final Map<Path, LockFile> registry = new ConcurrentHashMap<>();

	private final Path file;
	private final Path lockFile;
	private final Lock lock;

	private LockFile(Path file) {
		this.file = file;
		this.lockFile = file.resolveSibling(file.getFileName().toString() + SUFFIX);
		this.lock = new ReentrantLock();
	}

	public static LockFile of(Path file) {
		return registry.computeIfAbsent(file.toAbsolutePath(), LockFile::new);
	}
}
