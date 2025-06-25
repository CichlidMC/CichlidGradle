package fish.cichlidmc.cichlid_gradle.run;

import fish.cichlidmc.cichlid_gradle.util.ListPatch;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

public final class RunConfigurationImpl implements RunConfiguration {
	private final String name;
	private final Property<Type> type;
	private final Property<String> mainClass;
	private final DirectoryProperty runDir;
	private final Property<String> sourceSet;
	private final Property<ListPatch<String>> programArgs;
	private final Property<ListPatch<String>> jvmArgs;

	public RunConfigurationImpl(String name, ObjectFactory objects, ProjectLayout layout) {
		this.name = name;
		this.type = objects.property(Type.class);
		this.mainClass = objects.property(String.class);
		this.runDir = objects.directoryProperty().convention(layout.getProjectDirectory().dir("run"));
		this.sourceSet = objects.property(String.class).convention("main");
		this.programArgs = ListPatch.<String>property(objects).convention(new ListPatch<>());
		this.jvmArgs = ListPatch.<String>property(objects).convention(new ListPatch<>());
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Property<Type> getType() {
		return this.type;
	}

	@Override
	public Property<String> getMainClass() {
		return this.mainClass;
	}

	@Override
	public DirectoryProperty getRunDir() {
		return this.runDir;
	}

	@Override
	public Property<String> getSourceSet() {
		return this.sourceSet;
	}

	@Override
	public Property<ListPatch<String>> getProgramArgs() {
		return this.programArgs;
	}

	@Override
	public Property<ListPatch<String>> getJvmArgs() {
		return this.jvmArgs;
	}

	public void client() {
		this.getType().set(Type.CLIENT);
	}

	public void server() {
		this.getType().set(Type.SERVER);
	}

	public void jvmArg(String arg) {
		this.getJvmArgs().get().add(arg);
	}

	public void programArg(String arg) {
		this.getProgramArgs().get().add(arg);
	}

	public void sourceSet(SourceSet sourceSet) {
		this.getSourceSet().set(sourceSet.getName());
	}

	public static NamedDomainObjectFactory<RunConfigurationImpl> factory(Project project) {
		return name -> new RunConfigurationImpl(name, project.getObjects(), project.getLayout());
	}
}
