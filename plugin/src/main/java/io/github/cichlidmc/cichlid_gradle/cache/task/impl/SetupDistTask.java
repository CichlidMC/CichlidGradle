package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import io.github.cichlidmc.cichlid_gradle.util.DownloadBatch;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.version.download.Download;
import io.github.cichlidmc.pistonmetaparser.version.download.Downloads;
import net.neoforged.art.api.Renamer;
import net.neoforged.art.api.Transformer;
import net.neoforged.srgutils.IMappingFile;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;

public class SetupDistTask extends CacheTask {
	private final Distribution dist;
	private final VersionStorage storage;
	private final FullVersion version;

	public SetupDistTask(TaskContext context, Distribution dist, VersionStorage storage, FullVersion version) {
		super("Setup " + dist, "Setup " + version.id + " - " + dist, context);
		this.dist = dist;
		this.storage = storage;
		this.version = version;
		if (dist.isSpecial()) {
			throw new IllegalArgumentException("Invalid distribution: " + dist);
		}
	}

	@Override
	protected void doRun() throws IOException {
		if (this.dist == Distribution.CLIENT) {
			this.context.submit(new GenerateClientRunTemplateTask(this.context, this.storage.runs, this.version));
			this.context.submit(new ExtractNativesTask(this.context, this.storage.natives, this.version));
		}

		this.context.submit(new GenerateMetadataTask(this.context, this.dist, this.storage.jars, this.version));

		Downloads downloads = this.version.downloads;
		Download jarDownload = this.dist.choose(() -> downloads.client, downloads.server::get);
		Optional<Download> mappingsDownload = this.dist.choose(downloads.clientMappings, downloads.serverMappings);

		if (mappingsDownload.isEmpty()) {
			throw new IllegalArgumentException("Support for versions without mojmap is not yet implemented.");
		}

		// jar goes to a temp file first for remapping
		Path tempJar = this.storage.jars.temp(this.dist);
		Path jar = this.storage.jars.path(this.dist);
		Path mappings = this.storage.mappings.path(this.dist);

		new DownloadBatch.Builder()
				.download(jarDownload, tempJar)
				.download(mappingsDownload.get(), mappings)
				.build()
				.execute();

		if (this.dist == Distribution.SERVER) {
			this.tryUnbundle(tempJar);
		}

		// remap
		List<String> log = new ArrayList<>();
		// mojmap is distributed named -> obf, reverse it
		IMappingFile loadedMappings = IMappingFile.load(mappings.toFile()).reverse();
		Transformer.Factory transformer = Transformer.renamerFactory(loadedMappings, false);
		try (Renamer renamer = Renamer.builder().logger(log::add).add(transformer).build()) {
			renamer.run(tempJar.toFile(), jar.toFile());
		}

		// save remapping log, just in case it's important later
		Path logFile = this.storage.mappings.log(this.dist);
		Files.writeString(logFile, String.join("\n", log));
		Files.delete(tempJar);
		FileUtils.removeJarSignatures(jar);

		if (this.dist == Distribution.SERVER) {
			// do this after remapping, so the real jar is present
			this.context.submit(new GenerateServerRunTemplateTask(this.context, this.storage));
		}

		// TODO: decomp
	}

	private void tryUnbundle(Path serverTempJar) throws IOException {
		try (JarFile jarFile = new JarFile(serverTempJar.toFile())) {
			String format = jarFile.getManifest().getMainAttributes().getValue("Bundler-Format");
			if (format == null)
				return;

			if (!format.equals("1.0")) {
				logger.warn("Server bundle uses an untested format, this may not go well.");
			}
		}

		this.context.submit(new GenerateMetadataTask(this.context, Distribution.BUNDLER, this.storage.jars, this.version));

		// move bundler to it's correct location
		Path bundler = this.storage.jars.path(Distribution.BUNDLER);
		Files.createDirectories(bundler.getParent());
		Files.move(serverTempJar, bundler);

		// locate and extract server back to the temp jar
		try (FileSystem fs = FileSystems.newFileSystem(bundler)) {
			Path versions = fs.getPath("META-INF", "versions");
			Path versionDir = FileUtils.getSingleFileInDirectory(versions);
			if (versionDir == null)
				return;
			Path realServer = FileUtils.getSingleFileInDirectory(versionDir);
			if (realServer == null)
				return;

			// copy server to its correct location
			Files.copy(realServer, serverTempJar);
		}

		// TODO: decomp bundler
	}
}
