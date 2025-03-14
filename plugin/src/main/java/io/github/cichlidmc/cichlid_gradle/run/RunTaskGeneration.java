package io.github.cichlidmc.cichlid_gradle.run;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.cache.RunsStorage;
import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import io.github.cichlidmc.cichlid_gradle.util.ListPatch;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class RunTaskGeneration {
	public static void setup(Project project) {
		// ideally tasks.addAllLater would be used, but that's not allowed.
		project.afterEvaluate(RunTaskGeneration::generate);
	}

	private static void generate(Project project) {
		CichlidExtension extension = CichlidExtension.get(project);
		CichlidCache cache = CichlidCache.get(project);
		extension.getRuns().all(config -> generateTask(config, cache, project));
	}

	private static void generateTask(RunConfiguration config, CichlidCache cache, Project project) {
		String name = config.getName();
		String taskName = "run" + capitalizeFirstCharacter(name);

		String version = getOrThrow(config.getVersion(), name, "version");

		String templateName = config.getTemplate().orElse(name).get();
		Map<String, RunsStorage.DefaultRunConfig> templateMap = cache.runs.getDefaultRuns(version);
		RunsStorage.DefaultRunConfig template = templateMap.get(templateName);
		if (template == null) {
			throw new IllegalArgumentException(
					"There is no run config template named '" + templateName
					+ "' for version '" + version + "'. Options: " + templateMap.keySet()
			);
		}

		project.getTasks().register(taskName, JavaExec.class, task -> {
			task.setGroup("cichlid");
			task.setDescription("Runs Minecraft with the '" + name + "' configuration.");

			SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
			SourceSet sourceSet = sourceSets.getByName(config.getSourceSet().get());
			task.setClasspath(sourceSet.getRuntimeClasspath());

			String mainClass = config.getMainClass().orElse(template.mainClass()).get();
			task.getMainClass().set(mainClass);

			File runDir = project.file(config.getRunDir().get());
			task.setWorkingDir(runDir);
			task.doFirst($ -> runDir.mkdirs());

			Placeholders.DynamicContext ctx = new Placeholders.DynamicContext(
					runDir.toPath(),
					cache.natives.getDir(version),
					cache.assets.root
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

	private static <T> T getOrThrow(Provider<T> provider, String configName, String fieldName) {
		if (provider.isPresent()) {
			return provider.get();
		} else {
			throw new IllegalArgumentException("Run config " + configName + " does not have its " + fieldName + " set.");
		}
	}

	private static String capitalizeFirstCharacter(String s) {
		char first = s.charAt(0);
		char capital = Character.toUpperCase(first);
		return capital + s.substring(1);
	}
}
