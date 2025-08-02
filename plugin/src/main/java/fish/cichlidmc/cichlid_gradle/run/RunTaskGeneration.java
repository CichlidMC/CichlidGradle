package fish.cichlidmc.cichlid_gradle.run;

import fish.cichlidmc.cichlid_gradle.CichlidGradlePlugin;
import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinition;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinitionImpl;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.ListPatch;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.List;

public class RunTaskGeneration {
	public static void setup(Project project) {
		// ideally tasks.addAllLater would be used, but that's not allowed.
		project.afterEvaluate(RunTaskGeneration::generate);
	}

	private static void generate(Project project) {
		NamedDomainObjectContainer<MinecraftDefinition> defs = MinecraftDefinition.getExtension(project);
		CichlidCache cache = CichlidCache.get(project);
		defs.all(def -> def.getRuns().all(config -> generateTask((MinecraftDefinitionImpl) def, config, cache, project)));
	}

	private static void generateTask(MinecraftDefinitionImpl def, RunConfiguration config, CichlidCache cache, Project project) {
		String name = config.getName();
		// runMcClient
		String taskName = "run" + capitalizeFirstCharacter(def.getName()) + capitalizeFirstCharacter(name);

		String version = def.version();
		VersionStorage storage = cache.getVersion(version);

		Distribution dist = def.dist();
		RunConfiguration.Type type = config.getType().get();
		if (!type.isCompatibleWith(dist)) {
			throw new InvalidUserDataException("Run config type " + type + " cannot be used with distribution " + dist);
		}

		project.getTasks().register(taskName, JavaExec.class, task -> {
			task.setGroup("minecraft");
			task.setDescription(String.format("Runs Minecraft with the '%s' definition and '%s' configuration.", def.getName(), name));

			RunTemplate template = storage.runs.getOrThrow(type);

			SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
			SourceSet sourceSet = sourceSets.getByName(config.getSourceSet().get());
			task.setClasspath(sourceSet.getRuntimeClasspath());

			String mainClass = config.getMainClass().orElse(template.mainClass()).get();
			task.getMainClass().set(mainClass);

			File runDir = project.file(config.getRunDir().get());
			task.setWorkingDir(runDir);
			task.doFirst($ -> runDir.mkdirs());

			String distArg = switch (config.getType().get()) {
				case CLIENT -> "client";
				case SERVER -> "dedicated_server";
			};

			File cichlid = project.getConfigurations().getByName(CichlidGradlePlugin.CICHLID_CONFIGURATION).getSingleFile();
			task.jvmArgs("-javaagent:" + cichlid.getAbsolutePath() + "=dist=" + distArg + ",version=" + version);

			Placeholders.DynamicContext ctx = new Placeholders.DynamicContext(
					runDir.toPath(), storage.natives.root, cache.assets.root
			);
			task.args(getArgs(config.getProgramArgs(), template.programArgs(), ctx));
			task.jvmArgs(getArgs(config.getJvmArgs(), template.jvmArgs(), ctx));
		});
	}

	private static List<String> getArgs(Property<ListPatch<String>> config, List<String> template, Placeholders.DynamicContext ctx) {
		List<String> list = config.get().apply(template);
		Placeholders.fillDynamic(ctx, list);
		mergeSystemProperties(list);
		return list;
	}

	private static void mergeSystemProperties(List<String> args) {
		for (int i = 0; i < args.size(); i++) {
			String key = args.get(i);
			if (!key.startsWith("-D") || i + 1 >= args.size())
				continue;

			String value = args.get(i + 1);
			args.set(i, key + '=' + value);
			args.remove(i + 1);
		}
	}

	private static String capitalizeFirstCharacter(String s) {
		char first = s.charAt(0);
		char capital = Character.toUpperCase(first);
		return capital + s.substring(1);
	}
}
