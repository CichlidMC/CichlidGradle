package fish.cichlidmc.cichlid_gradle.util.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Interface for a file that is being written to for some operation that should only be performed once.
 * The file is doubly-locked to protect it from both the same process (other threads) and other processes.
 */
public final class WorkFile implements AutoCloseable {
	private static final Map<Path, Lock> locks = new ConcurrentHashMap<>();

	public final Path path;
	public final FileChannel channel;

	private final Lock lock;
	private final FileLock fileLock;

	private boolean committed;
	private boolean closed;

	public WorkFile(Path path, FileChannel channel, Lock lock, FileLock fileLock) {
		this.path = path;
		this.channel = channel;
		this.lock = lock;
		this.fileLock = fileLock;
	}

	public OutputStream newOutputStream() {
		// prevent early closing of the stream, because that will break the lock
		return new BufferedOutputStream(new NonCloseableOutputStream(Channels.newOutputStream(this.channel)));
	}

	/**
	 * Commit this file, closing it in the process.
	 */
	public void commit() throws IOException {
		this.assertOpen();
		this.committed = true;
		this.close();
	}

	/**
	 * Close this file. If not committed, it will be deleted.
	 */
	@Override
	public void close() throws IOException {
		if (this.closed)
			return;

		this.closed = true;

		if (!this.committed) {
			// reset any work that's been done
			this.channel.truncate(0);
		}

		this.fileLock.close();
		this.channel.close();
		this.lock.unlock();
	}

	private void assertOpen() {
		if (this.closed) {
			throw new IllegalStateException("WorkFile is already closed: " + this.path);
		}
	}

	public static WorkFile claim(Path path) throws IOException {
		Lock lock = locks.computeIfAbsent(path, $ -> new ReentrantLock());
		lock.lock();

		try {
			FileUtils.ensureCreated(path);
			FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE);
			FileLock fileLock = channel.lock();

			return new WorkFile(path, channel, lock, fileLock);
		} catch (IOException e) {
			lock.unlock();
			throw e;
		}
	}

	public static void doIfEmpty(Path path, IoConsumer<WorkFile> consumer) throws IOException {
		try (WorkFile file = claim(path)) {
			if (file.channel.size() == 0) {
				consumer.accept(file);
			}

			file.commit();
		}
	}

	public static <T> Optional<T> getIfEmpty(Path path, IoFunction<WorkFile, T> function) throws IOException {
		try (WorkFile file = claim(path)) {
			if (file.channel.size() == 0) {
				T value = function.apply(file);
				file.commit();
				return Optional.of(value);
			}

			file.commit();
		}

		return Optional.empty();
	}
}
