package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.run.RunTemplate;
import fish.cichlidmc.cichlid_gradle.util.Distribution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

public class GenerateServerRunTemplateTask extends CacheTask {
	private final VersionStorage storage;

	protected GenerateServerRunTemplateTask(CacheTaskEnvironment env, VersionStorage storage) {
		super("ServerRunTemplate", "Generate the server run config template", context);
		this.storage = storage;
	}

	@Override
	protected void doRun() throws IOException {
		RunTemplate template = this.generateServerTemplate();
		this.storage.runs.writeRun(this.storage.version, "server", template);
	}

	private RunTemplate generateServerTemplate() throws IOException {
		// read main class from server jar manifest
		Path jar = this.storage.jars.path(Distribution.SERVER);
		if (!Files.exists(jar)) {
			throw new IllegalStateException("Minecraft server jar is missing");
		}
		try (JarFile jarFile = new JarFile(jar.toFile())) {
			String mainClass = jarFile.getManifest().getMainAttributes().getValue("Main-Class");
			if (mainClass == null) {
				throw new IllegalStateException("Main-Class attribute is missing");
			}

			return new RunTemplate(mainClass, List.of("nogui"), List.of("-Xmx1G"));
		}
	}
}
