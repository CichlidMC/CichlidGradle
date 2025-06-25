package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.run.RunConfiguration;
import fish.cichlidmc.cichlid_gradle.run.RunTemplate;
import fish.cichlidmc.tinyjson.TinyJson;
import fish.cichlidmc.tinyjson.value.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RunTemplateStorage {
	private final Path root;

	RunTemplateStorage(Path root) {
		this.root = root;
	}

	public RunTemplate getOrThrow(RunConfiguration.Type type) {
		Path path = this.root.resolve(type.name + ".json");
		if (!Files.exists(path)) {
			throw new IllegalStateException("RunTemplate for " + type + " is missing");
		}

		JsonValue json = TinyJson.parseOrThrow(path);
		return RunTemplate.parse(json);
	}

	public void writeTemplate(String name, RunTemplate run) throws IOException {
		Path file = this.root.resolve(name + ".json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, run.encode().toString());
	}
}
