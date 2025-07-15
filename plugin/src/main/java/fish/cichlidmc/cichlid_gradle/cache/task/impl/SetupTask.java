package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.LockableStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.run.RunTemplate;
import fish.cichlidmc.cichlid_gradle.run.RunTemplateGenerator;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.io.DownloadBatch;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.distmarker.Dist;
import fish.cichlidmc.pistonmetaparser.version.download.Download;
import fish.cichlidmc.pistonmetaparser.version.download.Downloads;
import net.neoforged.art.api.Renamer;
import net.neoforged.art.api.SignatureStripperConfig;
import net.neoforged.art.api.SourceFixerConfig;
import net.neoforged.art.api.Transformer;
import net.neoforged.srgutils.IMappingFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class SetupTask extends CacheTask {
	private static final Logger logger = Logging.getLogger(SetupTask.class);

	private LockableStorage.Lock lock;

	public SetupTask(CacheTaskEnvironment env) {
		super("Setup " + env.dist, env);

		if (env.dist == Distribution.MERGED) {
			throw new IllegalArgumentException("Cannot create a SetupTask for MERGED");
		}
	}

	@Override
	protected String run() throws IOException {
		if (this.env.dist == Distribution.CLIENT) {
			if (ExtractNativesTask.shouldRun(this.env.version)) {
				this.env.submit(ExtractNativesTask::new);
			}
		}

		Downloads downloads = this.env.version.downloads;
		Download jarDownload = this.env.dist.choose(() -> downloads.client, downloads.server::get);
		// shouldn't throw since this is checked before even starting
		Download mappingsDownload = this.env.dist.choose(downloads.clientMappings, downloads.serverMappings).orElseThrow();

		VersionStorage storage = this.env.cache.getVersion(this.env.version.id);
		this.lock = storage.lockLoudly();

		Path jar = storage.jars.get(this.env.dist);
		// jar goes to a temp file first for remapping
		Path tempJar = FileUtils.createTempFile(jar);
		Path mappings = storage.mappings.path(this.env.dist);

		new DownloadBatch.Builder()
				.download(jarDownload, tempJar)
				.download(mappingsDownload, mappings)
				.build()
				.execute();

		if (this.env.dist == Distribution.SERVER) {
			this.tryUnbundle(tempJar, storage);
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
		Path logFile = storage.mappings.log(this.env.dist);
		Files.writeString(logFile, String.join("\n", log));
		Files.delete(tempJar);

		// do this after remapping, so the real server jar is present
		RunTemplate template = switch (this.env.dist) {
			case SERVER -> RunTemplateGenerator.generateServer(this.env.cache, this.env.version);
			case CLIENT -> RunTemplateGenerator.generateClient(this.env.version);
			case MERGED -> throw new IllegalStateException("Dist is merged?");
		};

		storage.runs.writeTemplate(this.env.dist.name, template);

		return null;
	}

	@Override
	protected void cleanup() throws IOException {
		if (this.lock != null) {
			this.lock.close();
		}
	}

	private void tryUnbundle(Path serverTempJar, VersionStorage storage) throws IOException {
		try (JarFile jarFile = new JarFile(serverTempJar.toFile())) {
			String format = jarFile.getManifest().getMainAttributes().getValue("Bundler-Format");
			if (format == null)
				return;

			if (!format.equals("1.0")) {
				logger.warn("Server bundle uses an untested format ({}), this may not go well.", format);
			}
		}

		// move bundler to it's correct location
		Path bundler = storage.jars.get(Dist.BUNDLER);
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
	}
}
