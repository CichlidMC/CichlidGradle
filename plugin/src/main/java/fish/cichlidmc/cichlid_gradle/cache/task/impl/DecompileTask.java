package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.cache.task.processor.ClassGroup;
import fish.cichlidmc.cichlid_gradle.cache.task.processor.JarProcessor;
import fish.cichlidmc.cichlid_gradle.util.Utils;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.plugins.JarPluginLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
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

	public DecompileTask(CacheTaskEnvironment env) {
		super("Decompile " + env.dist, env);
	}

	@Override
	protected String run() throws FileNotFoundException {
		Path input = this.env.cache.reassembledJars.binary(this.env.version.id, this.env.hash, this.env.dist);

		if (!Files.exists(input)) {
			this.env.submitAndAwait(ReassembleBinaryTask::new);
		}

		FileUtils.assertExists(input);
		closeVineflowerFilesystem();

		try (FileSystem fs = FileSystems.newFileSystem(input)) {
			Path root = FileUtils.getSingleRoot(fs);
			Map<String, ClassGroup> groups = JarProcessor.collectInput(root).groups();

			CacheResultSaver saver = new CacheResultSaver(this.env.cache.decompiledClasses, groups);
			Fernflower decompiler = new Fernflower(saver, PREFERENCES, IFernflowerLogger.NO_OP);

			decompiler.addSource(input.toFile());
			decompiler.decompileContext();
			decompiler.clearContext();

			return "Decompiled " + saver.totalClasses() + " class(es), " + saver.savedClasses() + " of which were uncached.";
		} catch (Throwable t) {
			throw new RuntimeException("Error while decompiling", t);
		}
	}

	private static void closeVineflowerFilesystem() {
		// vineflower loads plugins from its jar, and it opens that filesystem with newFileSystem.
		// but that throws if the filesystem is already open, which it will be after the first cold run.
		// close it now to avoid an ugly useless error later.

		try {
			// this is copied and pasted from JarPluginLoader
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
