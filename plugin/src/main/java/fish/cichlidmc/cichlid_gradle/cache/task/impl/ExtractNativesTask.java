package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import fish.cichlidmc.cichlid_gradle.util.Download;
import fish.cichlidmc.cichlid_gradle.util.FileUtils;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.version.library.Artifact;
import fish.cichlidmc.pistonmetaparser.version.library.Classifier;
import fish.cichlidmc.pistonmetaparser.version.library.Library;
import fish.cichlidmc.pistonmetaparser.version.library.Natives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

public class ExtractNativesTask extends CacheTask {
	public static final String TEMP_FILE = "temp-jar-for-extracting";
	// the version manifest specifies files that should be excluded from extraction,
	// but it's undocumented and a pain to handle, and it doesn't even matter in the end
	// as far as I can tell. The only relevant case is META-INF, which needs to be skipped
	// to avoid duplicates. META-INF also seems to be the only thing ever specified
	// as an exclusion, so it makes sense.
	// class files are also skipped, since we're just extracting the entire jar. Cuts out most files.
	public static final Predicate<String> FILE_FILTER = fileName -> !fileName.endsWith("META-INF") && !fileName.endsWith(".class");

	private final Path path;
	private final FullVersion version;

	protected ExtractNativesTask(TaskContext context, Path path, FullVersion version) {
		super("ExtractNatives", "Extract native dependencies", context);
		this.path = path;
		this.version = version;
	}

	@Override
	protected void doRun() throws IOException {
		Files.createDirectories(this.path);

		long startTime = System.currentTimeMillis();

		for (Library library : this.version.libraries) {
			Optional<Classifier> maybeClassifier = library.natives.flatMap(Natives::choose);
			if (maybeClassifier.isPresent()) {
				Classifier classifier = maybeClassifier.get();
				this.downloadAndExtract(classifier.artifact);
			}
		}

		long endTime = System.currentTimeMillis();
		long seconds = (endTime - startTime) / 1000;
		this.logger.quiet("Finished extracting natives in {} seconds.", seconds);
	}

	// note: rd-20090515 has the exact same jinput dependency twice for some reason, and is probably not the only weird one.
	// this information isn't really relevant after some refactoring, but it's good to keep it around.
	private void downloadAndExtract(Artifact artifact) throws IOException {
		Path tempJar = Files.createTempFile(this.path, TEMP_FILE, ".jar");
		new Download(artifact, tempJar).run();
		try {
			FileUtils.unzip(tempJar, this.path, FILE_FILTER);
		} finally {
			Files.delete(tempJar);
		}
	}
}
