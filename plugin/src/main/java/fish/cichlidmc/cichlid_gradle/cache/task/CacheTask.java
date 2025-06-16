package fish.cichlidmc.cichlid_gradle.cache.task;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

public abstract class CacheTask {
	public final String name;

	protected final CacheTaskEnvironment env;

	protected CacheTask(String name, CacheTaskEnvironment env) {
		this.name = name;
		this.env = env;
	}

	/**
	 * @return a feedback message to be printed with the completion notice
	 */
	@Nullable
	protected abstract String run() throws IOException;

	/**
	 * Perform any necessary cleanup, including in the case of an exception being thrown by {@link #run()}.
	 */
	protected void cleanup() throws IOException {
	}

	record Runner(CacheTask task, long startTime) implements Supplier<@Nullable String> {
		Runner(CacheTask task) {
			this(task, System.currentTimeMillis());
		}

		@Override
		@Nullable
		public String get() {
			try {
				String message = this.task.run();
				this.task.cleanup();
				return message;
			} catch (IOException e) {
				try {
					this.task.cleanup();
				} catch (IOException suppressed) {
					e.addSuppressed(suppressed);
				}

				throw new UncheckedIOException(e);
			}
		}
	}
}
