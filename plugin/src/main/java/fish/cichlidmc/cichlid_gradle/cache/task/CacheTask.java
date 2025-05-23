package fish.cichlidmc.cichlid_gradle.cache.task;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.io.UncheckedIOException;

public abstract class CacheTask implements Runnable {
	public final String name;

	protected final CacheTaskEnvironment env;
	protected final Logger logger;

	protected CacheTask(String name, CacheTaskEnvironment env) {
		this.name = name;
		this.env = env;
		this.logger = Logging.getLogger(this.getClass());
	}

	protected abstract void doRun() throws IOException;

	protected void cleanup() throws IOException {
	}

	@Override
	public final void run() {
		try {
			this.doRun();
			this.cleanup();
		} catch (IOException e) {
			try {
				this.cleanup();
			} catch (IOException suppressed) {
				e.addSuppressed(suppressed);
			}

			throw new UncheckedIOException(e);
		}
	}
}
