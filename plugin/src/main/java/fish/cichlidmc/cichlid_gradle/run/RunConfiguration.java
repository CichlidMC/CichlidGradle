package fish.cichlidmc.cichlid_gradle.run;

import javax.inject.Inject;

import fish.cichlidmc.cichlid_gradle.util.ListPatch;
import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

public abstract class RunConfiguration implements Named {
	public abstract Property<String> getVersion();
	public abstract Property<String> getTemplate();
	public abstract Property<String> getMainClass();
	public abstract Property<String> getRunDir();
	public abstract Property<String> getSourceSet();
	public abstract Property<ListPatch<String>> getProgramArgs();
	public abstract Property<ListPatch<String>> getJvmArgs();

	@Inject // inject name
	public RunConfiguration() {
		this.getRunDir().convention("run");
		this.getSourceSet().convention("main");
		this.getProgramArgs().convention(new ListPatch<>());
		this.getJvmArgs().convention(new ListPatch<>());
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
}
