package io.github.cichlidmc.cichlid_gradle.cache;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.cichlid_gradle.util.IterableStream;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.version.library.Classifier;
import io.github.cichlidmc.pistonmetaparser.version.library.Library;
import io.github.cichlidmc.pistonmetaparser.version.library.Natives;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class NativesStorage {
	public static final String TEMP_JAR = "temp-jar-for-extracting.jar";

	private static final Logger logger = Logging.getLogger(AssetStorage.class);

	private final Path root;

	NativesStorage(Path root) {
		this.root = root;
	}

	public Path getDir(String version) {
		return this.root.resolve(version);
	}

	void extractNatives(FullVersion version) throws IOException {
		Path dir = this.getDir(version.id);

		for (Library library : version.libraries) {
			Optional<Classifier> maybeClassifier = library.natives.flatMap(Natives::choose);
			if (maybeClassifier.isPresent()) {
				Classifier classifier = maybeClassifier.get();
				logger.quiet("Extracting natives for library {}: classifier {}", library.name, classifier.name);
				this.downloadAndExtract(classifier, dir);
			}
		}
	}

	private void downloadAndExtract(Classifier classifier, Path dir) throws IOException {
		Path tempJar = dir.resolve(TEMP_JAR);
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
						Path dest = dir.resolve(fileName);
						// rd-20090515 has the exact jinput dependency twice for some reason.
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
