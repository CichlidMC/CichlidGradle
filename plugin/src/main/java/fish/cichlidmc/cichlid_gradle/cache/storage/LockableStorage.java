package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.util.FileUtils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A storage that is doubly locked, both in-memory and through a lock file.
 * This is done to avoid interference from other CichlidGradle instances running
 * concurrently, both in the same Gradle process and outside of it.
 */
public abstract class LockableStorage {
	public static final String LOCK_FILE = ".lock";

	private static final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

	public final Path root;

	private final Path lockFile;
	private final ReentrantLock lock;

	protected LockableStorage(Path root) {
		this.root = root;
		this.lockFile = root.resolve(LOCK_FILE);
		String key = this.root.toAbsolutePath().toString();
		this.lock = locks.computeIfAbsent(key, $ -> new ReentrantLock());
	}

	/**
	 * Lock this storage, and return the lock object that can be used to release the lock.
	 * @throws IOException if file locking fails
	 */
	public Lock lock() throws IOException {
		return new Lock(this.lock, this.lockFile);
	}

	public static final class Lock implements AutoCloseable {
		private final ReentrantLock lock;
		private final FileChannel channel;
		private final FileLock fileLock;

		private boolean open = true;

		private Lock(ReentrantLock lock, Path file) throws IOException {
			this.lock = lock;
			lock.lock();

			FileUtils.ensureCreated(file);
			this.channel = FileChannel.open(file, StandardOpenOption.WRITE);
			this.fileLock = this.channel.lock();
		}

		@Override
		public void close() throws IOException {
			if (this.open) {
				this.open = false;
				this.lock.unlock();
				this.fileLock.close();
				this.channel.close();
			}
		}
	}
}
