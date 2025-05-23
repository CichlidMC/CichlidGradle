package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.tinycodecs.util.Either;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
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

	private static final Logger logger = Logging.getLogger(LockableStorage.class);

	private static final Map<Path, ReentrantLock> locks = new ConcurrentHashMap<>();

	public final Path root;

	private final Path lockFile;
	private final ReentrantLock lock;

	protected LockableStorage(Path root) {
		this.root = root;
		this.lockFile = root.resolve(LOCK_FILE);
		this.lock = locks.computeIfAbsent(this.root.toAbsolutePath(), _ -> new ReentrantLock());
	}

	/**
	 * Lock this storage, and return the lock object that can be used to release the lock.
	 * @throws IOException if file locking fails
	 */
	public Lock lock() throws IOException {
		return Lock.create(this.lock, this.lockFile);
	}

	public Either<Lock, Lock.Owner> tryLock() throws IOException {
		return Lock.tryCreate(this.lock, this.lockFile);
	}

	public Lock lockLoudly(String message) throws IOException {
		Either<Lock, Lock.Owner> either = this.tryLock();
		if (either.isLeft()) {
			return either.left();
		}

		Lock.Owner reason = either.right();
		logger.quiet(message, reason);
		return this.lock();
	}

	public static final class Lock implements AutoCloseable {
		private final ReentrantLock lock;
		private final FileChannel channel;
		private final FileLock fileLock;

		private boolean open = true;

		private Lock(ReentrantLock lock, FileChannel channel, FileLock fileLock) {
			this.lock = lock;
			this.channel = channel;
			this.fileLock = fileLock;
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

		private static Lock create(ReentrantLock lock, Path file) throws IOException {
			lock.lock();

			FileUtils.ensureCreated(file);
			FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE);
			FileLock fileLock = channel.lock();

			return new Lock(lock, channel, fileLock);
		}

		private static Either<Lock, Owner> tryCreate(ReentrantLock lock, Path file) throws IOException {
			if (!lock.tryLock()) {
				return Either.right(Owner.THIS_PROCESS);
			}

			FileUtils.ensureCreated(file);
			FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE);
			FileLock fileLock = channel.tryLock();

			if (fileLock == null) {
				lock.unlock();
				return Either.right(Owner.ANOTHER_PROCESS);
			}

			return Either.left(new Lock(lock, channel, fileLock));
		}

		public enum Owner {
			THIS_PROCESS, ANOTHER_PROCESS;

			private final String name = this.name().toLowerCase(Locale.ROOT).replace('_', ' ');

			@Override
			public String toString() {
				return this.name;
			}
		}
	}
}
