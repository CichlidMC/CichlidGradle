package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.LockableStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.NativesStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.util.io.Download;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.version.library.Artifact;
import fish.cichlidmc.pistonmetaparser.version.library.Classifier;
import fish.cichlidmc.pistonmetaparser.version.library.Natives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

	private LockableStorage.Lock lock;

	protected ExtractNativesTask(CacheTaskEnvironment env) {
		super("Extract natives", env);
	}

	@Override
	protected String run() throws IOException {
		NativesStorage natives = this.env.cache.getVersion(this.env.version.id).natives;
		this.lock = natives.lockLoudly();

		List<Classifier> classifiers = getClassifiers(this.env.version);
		for (Classifier classifier : classifiers) {
			downloadAndExtract(natives.root, classifier.artifact);
		}

		return "Extracted natives from " + classifiers.size() + " jar(s)";
	}

	@Override
	protected void cleanup() throws IOException {
		if (this.lock != null) {
			this.lock.close();
		}
	}

	// note: rd-20090515 has the exact same jinput dependency twice for some reason, and is probably not the only weird one.
	// this information isn't really relevant after some refactoring, but it's good to keep it around.
	private static void downloadAndExtract(Path output, Artifact artifact) throws IOException {
		Path tempJar = Files.createTempFile(output, TEMP_FILE, ".jar");
		try {
			new Download(artifact, tempJar).run();
			FileUtils.unzip(tempJar, output, FILE_FILTER);
		} finally {
			Files.delete(tempJar);
		}
	}

	public static boolean shouldRun(FullVersion version) {
		return !getClassifiers(version).isEmpty();
	}

	private static List<Classifier> getClassifiers(FullVersion version) {
		return version.libraries.stream().flatMap(lib -> lib.natives.flatMap(Natives::choose).stream()).toList();
	}
}
