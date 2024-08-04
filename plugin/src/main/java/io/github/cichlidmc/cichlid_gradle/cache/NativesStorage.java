package io.github.cichlidmc.cichlid_gradle.cache;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class NativesStorage {
	public static final String PATH = "minecraft/natives";
	public static final String TEMP_JAR = "temp-jar-for-extracting.jar";

	private static final Logger logger = Logging.getLogger(AssetStorage.class);

	private final Path root;

	private NativesStorage(Path root) {
		this.root = root;
	}

	static NativesStorage get(Path path) {
		return new NativesStorage(path.resolve(PATH));
	}

	public void extractNatives(FullVersion version) throws IOException {
		Path dir = this.root.resolve(version.id());

		for (FullVersion.Library library : version.libraries()) {

			FullVersion.Natives natives = library.natives().orElse(null);
			FullVersion.Classifiers classifiers = library.download().classifiers().orElse(null);

			if (natives == null || classifiers == null)
				continue;

			FullVersion.Artifact artifact = classifiers.get(natives);
			if (artifact == null)
				continue;

			logger.quiet("Extracting natives for library {}", library.name());
			this.downloadAndExtract(artifact, dir);
		}
	}

	private void downloadAndExtract(FullVersion.Artifact artifact, Path dir) throws IOException {
		Path tempJar = dir.resolve(TEMP_JAR);
		FileUtils.downloadSilently(artifact, tempJar);

		try (FileSystem fs = FileSystems.newFileSystem(tempJar)) {
			// jar should have 1 root
			Path root = fs.getRootDirectories().iterator().next();
			try (Stream<Path> stream = Files.list(root)) {
				// the version manifest specifies files that should be excluded from extraction,
				// but it's undocumented and a pain to handle, and it doesn't even matter in the end.
				for (Iterator<Path> itr = stream.iterator(); itr.hasNext();) {
					Path file = itr.next();
					Path dest = dir.resolve(file.getFileName().toString());
					Files.copy(file, dest);
				}
			}
		}

		Files.delete(tempJar);
	}
}
