package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.storage.NativesStorage;
import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.cichlid_gradle.util.IterableStream;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.version.library.Classifier;
import io.github.cichlidmc.pistonmetaparser.version.library.Library;
import io.github.cichlidmc.pistonmetaparser.version.library.Natives;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ExtractNativesTask extends CacheTask {
	public static final String TEMP_JAR = "temp-jar-for-extracting.jar";

	private final NativesStorage storage;
	private final FullVersion version;

	protected ExtractNativesTask(TaskContext context, NativesStorage storage, FullVersion version) {
		super("ExtractNatives", "Extract native dependencies", context);
		this.storage = storage;
		this.version = version;
	}

	@Override
	protected void doRun() throws IOException {
		Path folder = this.storage.folder(this.version.id);

		for (Library library : this.version.libraries) {
			Optional<Classifier> maybeClassifier = library.natives.flatMap(Natives::choose);
			if (maybeClassifier.isPresent()) {
				Classifier classifier = maybeClassifier.get();
				this.downloadAndExtract(classifier, folder);
			}
		}
	}

	private void downloadAndExtract(Classifier classifier, Path folder) throws IOException {
		Path tempJar = folder.resolve(TEMP_JAR);
		FileUtils.downloadSilently(classifier.artifact, tempJar);

		try (FileSystem fs = FileSystems.newFileSystem(tempJar)) {
			// jar should have 1 root
			Path root = fs.getRootDirectories().iterator().next();
			try (IterableStream<Path> files = new IterableStream<>(Files.list(root))) {
				for (Path file : files) {
					// the version manifest specifies files that should be excluded from extraction,
					// but it's undocumented and a pain to handle, and it doesn't even matter in the end
					// as far as I can tell. The only relevant case is META-INF, which needs to be skipped
					// to avoid duplicates. META-INF also seems to be the only thing ever specified
					// as an exclusion, so it makes sense.
					String fileName = file.getFileName().toString();
					if (!fileName.endsWith("META-INF")) {
						Path dest = folder.resolve(fileName);
						// rd-20090515 has the exact same jinput dependency twice for some reason.
						// it's probably not the only weird one.
						if (!Files.exists(dest)) {
							Files.copy(file, dest);
						}
					}
				}
			}
		}

		Files.delete(tempJar);
	}
}
