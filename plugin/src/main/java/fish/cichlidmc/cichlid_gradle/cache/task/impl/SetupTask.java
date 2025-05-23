package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.io.DownloadBatch;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.version.download.Download;
import fish.cichlidmc.pistonmetaparser.version.download.Downloads;
import net.neoforged.art.api.Renamer;
import net.neoforged.art.api.SignatureStripperConfig;
import net.neoforged.art.api.SourceFixerConfig;
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

public class SetupTask extends CacheTask {
	public SetupTask(CacheTaskEnvironment env) {
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

		Renamer.Builder builder = Renamer.builder();
		builder.logger(log::add);
		builder.add(Transformer.renamerFactory(loadedMappings, false));
		builder.add(Transformer.sourceFixerFactory(SourceFixerConfig.JAVA));
		builder.add(Transformer.recordFixerFactory());
		builder.add(Transformer.parameterAnnotationFixerFactory());
		builder.add(Transformer.parameterFinalFlagRemoverFactory());
		builder.add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL));

		try (Renamer renamer = builder.build()) {
			renamer.run(tempJar.toFile(), jar.toFile());
		}

		// save remapping log, just in case it's important later
		Path logFile = this.storage.mappings.log(this.dist);
		Files.writeString(logFile, String.join("\n", log));
		Files.delete(tempJar);

		if (this.dist == Distribution.SERVER) {
			// do this after remapping, so the real jar is present
			this.context.submit(new GenerateServerRunTemplateTask(this.context, this.storage));
		}

		this.context.submit(new DecompileTask(this.context, this.dist, jar));
	}

	private void tryUnbundle(Path serverTempJar) throws IOException {
		try (JarFile jarFile = new JarFile(serverTempJar.toFile())) {
			String format = jarFile.getManifest().getMainAttributes().getValue("Bundler-Format");
			if (format == null)
				return;

			if (!format.equals("1.0")) {
				this.logger.warn("Server bundle uses an untested format, this may not go well.");
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

		this.context.submit(new DecompileTask(this.context, Distribution.BUNDLER, bundler));
	}
}
