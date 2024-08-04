package io.github.cichlidmc.cichlid_gradle.run;

import org.gradle.api.Named;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public interface RunConfiguration extends Named {
	Property<String> getMainClass();
	ListProperty<String> getProgramArgs();
	ListProperty<String> getJvmArgs();

	void copyFrom(String other);
}
