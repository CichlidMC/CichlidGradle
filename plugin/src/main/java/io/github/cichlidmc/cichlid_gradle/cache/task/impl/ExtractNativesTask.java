package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.version.library.Artifact;
import io.github.cichlidmc.pistonmetaparser.version.library.Classifier;
import io.github.cichlidmc.pistonmetaparser.version.library.Library;
import io.github.cichlidmc.pistonmetaparser.version.library.Natives;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

public class ExtractNativesTask extends CacheTask {
	public static final String TEMP_JAR = "temp-jar-for-extracting.jar";

	private final Path path;
	private final FullVersion version;

	protected ExtractNativesTask(TaskContext context, Path path, FullVersion version) {
		super("ExtractNatives", "Extract native dependencies", context);
		this.path = path;
		this.version = version;
	}

	@Override
	protected void doRun() throws IOException {
		for (Library library : this.version.libraries) {
			if (library.artifact.isPresent()) {
				this.downloadAndExtract(library.artifact.get());
			}

			Optional<Classifier> maybeClassifier = library.natives.flatMap(Natives::choose);
			if (maybeClassifier.isPresent()) {
				Classifier classifier = maybeClassifier.get();
				this.downloadAndExtract(classifier.artifact);
			}
		}
	}

	// note: rd-20090515 has the exact same jinput dependency twice for some reason, and is probably not the only weird one.
	// this information isn't really relevant after some refactoring, but it's good to keep it around.
	private void downloadAndExtract(Artifact artifact) throws IOException {
		Path tempJar = this.path.resolve(TEMP_JAR);
		FileUtils.downloadSilently(artifact, tempJar);

		try (FileSystem fs = FileSystems.newFileSystem(tempJar)) {
			// jar should have 1 root
			Path root = fs.getRootDirectories().iterator().next();
			Files.walkFileTree(root, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					// the version manifest specifies files that should be excluded from extraction,
					// but it's undocumented and a pain to handle, and it doesn't even matter in the end
					// as far as I can tell. The only relevant case is META-INF, which needs to be skipped
					// to avoid duplicates. META-INF also seems to be the only thing ever specified
					// as an exclusion, so it makes sense.
					// class files are also skipped, since we're just extracting the entire jar. Cuts out most files.
					String fileName = file.getFileName().toString();
					if (!fileName.endsWith("META-INF") && !fileName.endsWith(".class")) {
						Path relative = root.relativize(file);
						Path dest = ExtractNativesTask.this.path.resolve(relative.toString());
						if (!Files.exists(dest)) {
							Files.createDirectories(dest.getParent());
							Files.copy(file, dest);
						}
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}

		Files.delete(tempJar);
	}
}
