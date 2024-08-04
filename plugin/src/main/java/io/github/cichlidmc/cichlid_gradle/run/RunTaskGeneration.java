package io.github.cichlidmc.cichlid_gradle.run;

import java.util.concurrent.Callable;

import io.github.cichlidmc.cichlid_gradle.extension.CichlidExtension;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.internal.JavaPluginHelper;
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal;
import org.gradle.api.tasks.JavaExec;

public class RunTaskGeneration {
	public static void run(Project project) {
		CichlidExtension extension = project.getExtensions().getByType(CichlidExtension.class);
		extension.getRuns().all(config -> generateTask(config, project));
	}

	private static void generateTask(RunConfiguration config, Project project) {
		String taskName = "run" + capitalizeFirstCharacter(config.getName());
		project.getTasks().create(taskName, JavaExec.class, task -> {
			task.setGroup("cichlid");
			task.setDescription("Runs Minecraft with the " + config.getName() + " configuration.");

			// based on the Application plugin
			JvmFeatureInternal mainFeature = JavaPluginHelper.getJavaComponent(project).getMainFeature();
			FileCollection runtimeClasspath = project.files().from(new RuntimeClasspathProvider(task, mainFeature));
			task.setClasspath(runtimeClasspath);

			task.getMainClass().set(config.getMainClass());
			task.setArgs(config.getProgramArgs().get());
			task.getJvmArguments().set(config.getJvmArgs());
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
