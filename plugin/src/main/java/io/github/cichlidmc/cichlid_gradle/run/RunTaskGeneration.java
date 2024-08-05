package io.github.cichlidmc.cichlid_gradle.run;

import java.util.concurrent.Callable;

import io.github.cichlidmc.cichlid_gradle.cache.RunsStorage;
import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import io.github.cichlidmc.cichlid_gradle.util.HierarchicalListImpl;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.internal.JavaPluginHelper;
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal;
import org.gradle.api.tasks.JavaExec;

public class RunTaskGeneration {
	public static void run(String mcVer, RunsStorage runsStorage, Project project) {
		CichlidExtension extension = CichlidExtension.get(project);
		NamedDomainObjectContainer<RunConfiguration> runs = extension.getRuns();
		addDefaultRuns(mcVer, runsStorage, runs);
		runs.all(config -> generateTask(config, runs, project));
	}

	private static void addDefaultRuns(String mcVer, RunsStorage runsStorage, NamedDomainObjectContainer<RunConfiguration> runs) {
		runsStorage.getRuns(mcVer).forEach(run -> runs.register(run.name(), config -> {
			config.getMainClass().set(run.mainClass());
			config.getProgramArgs().get().addAll(run.programArgs());
			config.getJvmArgs().get().addAll(run.jvmArgs());
		}));
	}

	private static void generateTask(RunConfiguration config, NamedDomainObjectContainer<RunConfiguration> runs, Project project) {
		String taskName = "run" + capitalizeFirstCharacter(config.getName());
		project.getTasks().create(taskName, JavaExec.class, task -> {
			task.setGroup("cichlid");
			task.setDescription("Runs Minecraft with the " + config.getName() + " configuration.");

			// based on the Application plugin
			JvmFeatureInternal mainFeature = JavaPluginHelper.getJavaComponent(project).getMainFeature();
			FileCollection runtimeClasspath = project.files().from(new RuntimeClasspathProvider(task, mainFeature));
			task.setClasspath(runtimeClasspath);

			if (config.getParent().isPresent()) {
				RunConfiguration parent = runs.getByName(config.getParent().get());
				HierarchicalListImpl.setParent(config.getProgramArgs(), parent.getProgramArgs());
				HierarchicalListImpl.setParent(config.getJvmArgs(), parent.getJvmArgs());
				if (!config.getMainClass().isPresent()) {
					config.getMainClass().set(parent.getMainClass());
				}
			}

			task.getMainClass().set(config.getMainClass());
			task.setArgs(HierarchicalListImpl.resolve(config.getProgramArgs()));
			task.getJvmArguments().set(HierarchicalListImpl.resolve(config.getJvmArgs()));
		});
	}

	private static String capitalizeFirstCharacter(String s) {
		char first = s.charAt(0);
		char capital = Character.toUpperCase(first);
		return capital + s.substring(1);
	}

	private record RuntimeClasspathProvider(JavaExec task, JvmFeatureInternal mainFeature) implements Callable<FileCollection> {
		@Override
		public FileCollection call() {
			if (task.getMainModule().isPresent()) {
				return jarsOnlyRuntimeClasspath(mainFeature);
			} else {
				return runtimeClasspath(mainFeature);
			}
		}
	}

	// from Application plugin

	private static FileCollection runtimeClasspath(JvmFeatureInternal mainFeature) {
		return mainFeature.getSourceSet().getRuntimeClasspath();
	}

	private static FileCollection jarsOnlyRuntimeClasspath(JvmFeatureInternal mainFeature) {
		return mainFeature.getJarTask().get().getOutputs().getFiles().plus(mainFeature.getRuntimeClasspathConfiguration());
	}
}
