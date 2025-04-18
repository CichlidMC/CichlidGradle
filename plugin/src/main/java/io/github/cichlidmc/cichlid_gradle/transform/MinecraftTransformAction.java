package io.github.cichlidmc.cichlid_gradle.transform;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.InputArtifactDependencies;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform action that applies Sushi transformers to Minecraft and generates a sources jar.
 */
@CacheableTransform
public abstract class MinecraftTransformAction implements TransformAction<TransformParameters.None> {
	private static final Logger logger = Logging.getLogger(MinecraftTransformAction.class);
	private static final Map<String, Object> decompileSettings = getDecompileSettings();

	@InputArtifact
	@PathSensitive(PathSensitivity.NAME_ONLY)
	public abstract Provider<FileSystemLocation> getInput();

	@InputArtifactDependencies
	@PathSensitive(PathSensitivity.NAME_ONLY)
	public abstract FileCollection getDependencies();

	@Override
	public void transform(TransformOutputs outputs) {
		File file = this.getInput().get().getAsFile();
		System.out.println("transforming: " + file);
		String name = file.getName();

		if (isNotMinecraft(file) || true) {
			// outputs.file(file);

			String newName = name.replace(".jar", "-transformed.jar");
			File output = outputs.file(newName);
			copy(file, output);

			return;
		}

		logger.quiet("Minecraft transform is not cached; transforming: " + name);
		// for (File dep : this.getDependencies()) {
		// 	logger.quiet("\t- " + dep.getName());
		// }

		String newName = name.replace(".jar", "-transformed.jar");
		File output = outputs.file(newName);
		copy(file, output);

		// generate sources
		closeVfFilesystem();

		String sourcesName = name.replace(".jar", "-transformed-sources.jar");
		File sources = outputs.file(sourcesName);
		IResultSaver saver = new SingleFileSaver(sources);
		Fernflower decompiler = new Fernflower(saver, decompileSettings, IFernflowerLogger.NO_OP);

		logger.quiet("Starting decompile. This could take a bit...");
		decompiler.addSource(output);
		decompiler.decompileContext();
		decompiler.clearContext();
		logger.quiet("Decompile finished.");

		List<String> otherNames = List.of(
				name.replace(".jar", "-sources.jar")
		);

		for (String h : otherNames) {
			copy(sources, outputs.file(h));
		}
	}

	private static void copy(File from, File to) {
		try {
			Files.copy(from.toPath(), to.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isNotMinecraft(File file) {
		String name = file.getName();
		if (!name.endsWith(".jar"))
			return true;

		for (String module : CichlidCache.MINECRAFT_MODULES) {
			if (name.startsWith(module + '-')) {
				return false;
			}
		}

		return true;
	}

	private static Map<String, Object> getDecompileSettings() {
		Map<String, Object> settings = new HashMap<>(IFernflowerPreferences.DEFAULTS);
		settings.put(IFernflowerPreferences.INDENT_STRING, "\t");
		return settings;
	}

	private static void closeVfFilesystem() {
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
