package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.run.RunTemplate;
import fish.cichlidmc.tinyjson.TinyJson;
import fish.cichlidmc.tinyjson.value.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RunTemplateStorage {
	private final Path root;

	RunTemplateStorage(Path root) {
		this.root = root;
	}

	public Map<String, RunTemplate> getTemplates(String version) {
		Path dir = this.dir(version);
		if (!Files.exists(dir))
			return Map.of();

		try (Stream<Path> stream = Files.list(dir)) {
			Map<String, RunTemplate> map = new HashMap<>();
			stream.forEach(path -> {
				String name = extractName(path);
				RunTemplate config = readRun(path);
				map.put(name, config);
			});
			return map;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeRun(String version, String name, RunTemplate run) throws IOException {
		Path file = this.dir(version).resolve(name + ".json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, run.encode().toString());
	}

	private Path dir(String version) {
		return this.root.resolve(version);
	}

	private RunTemplate readRun(Path file) {
		JsonValue json = TinyJson.parseOrThrow(file);
		return RunTemplate.parse(json);
	}

	private static String extractName(Path path) {
		String fileName = path.getFileName().toString();
		int dot = fileName.indexOf(".");
		return dot == -1 ? fileName : fileName.substring(0, dot);
	}

}
