package io.github.cichlidmc.cichlid_gradle.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class RunsStorage {
	private static final Logger logger = Logging.getLogger(RunsStorage.class);
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
			if (version.downloads().server().isPresent()) {
				this.generateServerRun(version, maven);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void generateClientRun(FullVersion version) throws IOException {
		String mainClass = version.mainClass();
		List<String> programArgs = new ArrayList<>();
		List<String> jvmArgs = new ArrayList<>();

		// TODO: account for all placeholders, needs some analysis

		if (version.splitArgs().isPresent()) {
			FullVersion.SplitArguments args = version.splitArgs().get();
			for (FullVersion.Argument arg : args.game()) {
				if (FullVersion.Rule.test(arg.rules(), FullVersion.Features.EMPTY)) {
					programArgs.addAll(arg.values());
				}
			}
		} else if (version.stringArgs().isPresent()) {
			String args = version.stringArgs().get().value();
		}
	}

	private void generateServerRun(FullVersion version, MinecraftMaven maven) throws IOException {
		// read main class from server jar manifest
		Path jar = maven.artifact("minecraft-server", version.id(), "jar");
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
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			JsonElement json = JsonParser.parseReader(reader);
			return Run.CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeRun(FullVersion version, Run run) throws IOException {
		JsonElement json = Run.CODEC.encodeStart(JsonOps.INSTANCE, run).getOrThrow();
		String string = gson.toJson(json);

		Path file = this.dir(version.id()).resolve(run.name + ".json");
		Files.createDirectories(file.getParent());
		Files.writeString(file, string);
	}

	public record Run(String name, String mainClass, List<String> programArgs, List<String> jvmArgs) {
		public static final Codec<Run> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("name").forGetter(Run::name),
				Codec.STRING.fieldOf("main_class").forGetter(Run::mainClass),
				Codec.STRING.listOf().fieldOf("program_args").forGetter(Run::programArgs),
				Codec.STRING.listOf().fieldOf("jvm_args").forGetter(Run::jvmArgs)
		).apply(instance, Run::new));
	}
}
