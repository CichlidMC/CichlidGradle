package io.github.cichlidmc.cichlid_gradle.cache.storage;

import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.cache.task.impl.SetupTask;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.pistonmetaparser.FullVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VersionStorage extends LockableStorage {
	public static final String COMPLETE_MARKER = ".complete";

	public final String version;

	public final MappingsStorage mappings;
	public final JarsStorage jars;
	public final Path natives;
	public final RunTemplateStorage runs;

	public final Path completeMarker;

	public VersionStorage(Path root, String version) {
		super(root);
		this.version = version;
		this.mappings = new MappingsStorage(root.resolve("mappings"));
		this.jars = new JarsStorage(root.resolve("jars"), version);
		this.natives = root.resolve("natives");
		this.runs = new RunTemplateStorage(root.resolve("runs"));
		this.completeMarker = root.resolve(COMPLETE_MARKER);
	}

	public void submitInitialTasks(FullVersion version, TaskContext context) {
		SetupTask task = new SetupTask(context, this, version);
		context.submitSilently(task);
	}

	public boolean isComplete() {
		return Files.exists(this.completeMarker);
	}

	public void markComplete() {
		try {
			FileUtils.ensureCreated(this.completeMarker);
		} catch (IOException e) {
			// really? failing at the last possible step?
			throw new RuntimeException(e);
		}
	}
}
