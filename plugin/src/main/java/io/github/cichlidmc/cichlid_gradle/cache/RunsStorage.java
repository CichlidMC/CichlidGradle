package io.github.cichlidmc.cichlid_gradle.cache;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class RunsStorage {
	public static final String PATH = "minecraft/runs";

	private static final Logger logger = Logging.getLogger(RunsStorage.class);

	private final Path root;

	private RunsStorage(Path root) {
		this.root = root;
	}

	static RunsStorage get(Path path) {
		return new RunsStorage(path.resolve(PATH));
	}

	public List<Run> getRuns(String version) {
		return null;
	}

	public void generateRuns(FullVersion version) {
		// server
		if (version.downloads().server().isPresent()) {

		}
		// client
		String mainClass = version.mainClass();
		List<String> programArgs = new ArrayList<>();
		List<String> jvmArgs = new ArrayList<>();

		if (version.splitArgs().isPresent()) {
			FullVersion.SplitArguments args = version.splitArgs().get();
			for (FullVersion.Argument arg : args.game()) {
				if (FullVersion.Rule.test(arg.rules(), FullVersion.Features.EMPTY)) {
					programArgs.addAll(arg.values());
				}
			}
		}
	}

	public record Run(String mainClass, List<String> programArgs, List<String> jvmArgs) {
		public static final Codec<Run> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("main_class").forGetter(Run::mainClass),
				Codec.STRING.listOf().fieldOf("program_args").forGetter(Run::programArgs),
				Codec.STRING.listOf().fieldOf("jvm_args").forGetter(Run::jvmArgs)
		).apply(instance, Run::new));
	}
}
