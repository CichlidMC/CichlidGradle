package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.Utils;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.main.plugins.JarPluginLoader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DecompileTask extends CacheTask {
	public static final Map<String, Object> PREFERENCES = Utils.make(() -> {
		Map<String, Object> preferences = new HashMap<>(IFernflowerPreferences.DEFAULTS);
		preferences.put(IFernflowerPreferences.INDENT_STRING, "\t");
		return Collections.unmodifiableMap(preferences);
	});

	private final Path jar;

	public DecompileTask(TaskContext context, Distribution dist, Path jar) {
		super("Decompile " + dist, "Decompiling " + dist, context);
		this.jar = jar;
	}

	@Override
	protected void doRun() throws IOException {
		String outputName = this.jar.getFileName().toString().replace(".jar", "-sources.jar");
		Path output = this.jar.resolveSibling(outputName);

		closeVineflowerFilesystem();

		IResultSaver saver = new SingleFileSaver(output.toFile());
		Fernflower decompiler = new Fernflower(saver, PREFERENCES, IFernflowerLogger.NO_OP);
		try {
			decompiler.addSource(this.jar.toFile());
			decompiler.decompileContext();
			decompiler.clearContext();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static void closeVineflowerFilesystem() {
		// vineflower loads plugins from its jar, and it opens that filesystem with newFileSystem.
		// but that throws if the filesystem is already open, which it will be after the first cold run.
		// close it now to avoid an ugly useless error later.

		try {
			File vfJar = new File(JarPluginLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (vfJar.exists() && !vfJar.isDirectory() && vfJar.getPath().endsWith(".jar")) {
				URI uri = URI.create("jar:" + vfJar.toURI());
				// this will throw if it's not open
				FileSystem fs = FileSystems.getFileSystem(uri);

				fs.close();
			}
		} catch (Exception ignored) {}
	}
}
