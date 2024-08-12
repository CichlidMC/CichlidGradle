package io.github.cichlidmc.cichlid_gradle.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

	public List<Run> getRuns(String version) {
		Path dir = this.dir(version);
		if (!Files.exists(dir))
			return List.of();

		try (Stream<Path> stream = Files.list(dir)) {
			return stream.map(this::readRun).toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void generateRuns(FullVersion version, MinecraftMaven maven) {
		try {
			this.generateClientRun(version);
			if (version.downloads.server.isPresent()) {
				this.generateServerRun(version, maven);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void generateClientRun(FullVersion version) throws IOException {
		String mainClass = version.mainClass;
		List<String> programArgs = new ArrayList<>();
		List<String> jvmArgs = new ArrayList<>();

		// TODO: account for all placeholders, needs some analysis


	}

	private void generateServerRun(FullVersion version, MinecraftMaven maven) throws IOException {
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

			Run run = new Run("server", mainClass, List.of("nogui"), List.of("-Xmx1G"));
			this.writeRun(version, run);
		}
	}

	private Path dir(String version) {
		return this.root.resolve(version);
	}

	private Run readRun(Path file) {
		return Run.parse(TinyJson.parseOrThrow(file));
	}

	private void writeRun(FullVersion version, Run run) throws IOException {
		Path file = this.dir(version.id).resolve(run.name + ".json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, run.encode().toString());
	}

	public record Run(String name, String mainClass, List<String> programArgs, List<String> jvmArgs) {
		public JsonObject encode() {
			JsonObject json = new JsonObject();
			json.put("name", this.name);
			json.put("main_class", this.mainClass);
			json.put("program_args", JsonArray.of(this.programArgs.stream().map(JsonString::new).toArray(JsonValue[]::new)));
			json.put("jvm_args", JsonArray.of(this.jvmArgs.stream().map(JsonString::new).toArray(JsonValue[]::new)));
			return json;
		}

		public static Run parse(JsonValue value) {
			JsonObject json = value.asObject();

			String name = json.get("name").asString().value();
			String mainClass = json.get("main_class").asString().value();
			List<String> programArgs = json.get("program_args").asArray().stream()
					.map(arg -> arg.asString().value())
					.toList();
			List<String> jvmArgs = json.get("jvm_args").asArray().stream()
					.map(arg -> arg.asString().value())
					.toList();

			return new Run(name, mainClass, programArgs, jvmArgs);
		}
	}
}
