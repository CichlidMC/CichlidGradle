package io.github.cichlidmc.cichlid_gradle.run;

import javax.inject.Inject;

import io.github.cichlidmc.cichlid_gradle.util.HierarchicalList;
import io.github.cichlidmc.cichlid_gradle.util.HierarchicalListImpl;
import org.gradle.api.Named;
import org.gradle.api.provider.Property;

public abstract class RunConfiguration implements Named {
	public abstract Property<String> getParent();
	public abstract Property<String> getMainClass();
	public abstract Property<HierarchicalList<String>> getProgramArgs();
	public abstract Property<HierarchicalList<String>> getJvmArgs();

	@Inject // inject name
	public RunConfiguration() {
		this.getProgramArgs().convention(new HierarchicalListImpl<>());
		this.getJvmArgs().convention(new HierarchicalListImpl<>());
	}

	public void jvmArg(String arg) {
		this.getJvmArgs().get().add(arg);
	}

	public void programArg(String arg) {
		this.getProgramArgs().get().add(arg);
	}
}
