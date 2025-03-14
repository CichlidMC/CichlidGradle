package io.github.cichlidmc.cichlid_gradle.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.tinyjson.TinyJson;
import io.github.cichlidmc.tinyjson.value.JsonValue;
import io.github.cichlidmc.tinyjson.value.composite.JsonArray;
import io.github.cichlidmc.tinyjson.value.composite.JsonObject;
import io.github.cichlidmc.tinyjson.value.primitive.JsonString;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class RunsStorage {
	private static final Logger logger = Logging.getLogger(RunsStorage.class);

	private final Path root;

	RunsStorage(Path root) {
		this.root = root;
	}

	public Map<String, DefaultRunConfig> getDefaultRuns(String version) {
		Path dir = this.dir(version);
		if (!Files.exists(dir))
			return Map.of();

		try (Stream<Path> stream = Files.list(dir)) {
			Map<String, DefaultRunConfig> map = new HashMap<>();
			stream.forEach(path -> {
				String name = extractName(path);
				DefaultRunConfig config = readRun(path);
				map.put(name, config);
			});
			return map;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void generateRuns(FullVersion version, MinecraftMaven maven) {
		try {
			DefaultRunConfig client = ClientRunGenerator.generate(version);
			this.writeRun(version, "client", client);
			if (version.downloads.server.isPresent()) {
				DefaultRunConfig server = this.generateServerRun(version, maven);
				this.writeRun(version, "server", server);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DefaultRunConfig generateServerRun(FullVersion version, MinecraftMaven maven) throws IOException {
		// read main class from server jar manifest
		Path jar = maven.artifact("minecraft-server", version.id, "jar");
		if (!Files.exists(jar)) {
			throw new IllegalStateException("Minecraft server jar is missing");
		}
		try (JarFile jarFile = new JarFile(jar.toFile())) {
			String mainClass = jarFile.getManifest().getMainAttributes().getValue("Main-Class");
			if (mainClass == null) {
				throw new IllegalStateException("Main-Class attribute is missing");
			}

			return new DefaultRunConfig(mainClass, List.of("nogui"), List.of("-Xmx1G"));
		}
	}

	private Path dir(String version) {
		return this.root.resolve(version);
	}

	private DefaultRunConfig readRun(Path file) {
		String name = extractName(file);
		JsonValue json = TinyJson.parseOrThrow(file);
		return DefaultRunConfig.parse(name, json);
	}

	private void writeRun(FullVersion version, String name, DefaultRunConfig run) throws IOException {
		Path file = this.dir(version.id).resolve(name + ".json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, run.encode().toString());
	}

	private static String extractName(Path path) {
		String fileName = path.getFileName().toString();
		int dot = fileName.indexOf(".");
		return dot == -1 ? fileName : fileName.substring(0, dot);
	}

	public record DefaultRunConfig(String mainClass, List<String> programArgs, List<String> jvmArgs) {
		public JsonObject encode() {
			JsonObject json = new JsonObject();
			json.put("main_class", this.mainClass);
			json.put("program_args", JsonArray.of(this.programArgs.stream().map(JsonString::new).toArray(JsonValue[]::new)));
			json.put("jvm_args", JsonArray.of(this.jvmArgs.stream().map(JsonString::new).toArray(JsonValue[]::new)));
			return json;
		}

		public static DefaultRunConfig parse(String name, JsonValue value) {
			JsonObject json = value.asObject();

			String mainClass = json.get("main_class").asString().value();
			List<String> programArgs = json.get("program_args").asArray().stream()
					.map(arg -> arg.asString().value())
					.toList();
			List<String> jvmArgs = json.get("jvm_args").asArray().stream()
					.map(arg -> arg.asString().value())
					.toList();

			return new DefaultRunConfig(mainClass, programArgs, jvmArgs);
		}
	}
}
