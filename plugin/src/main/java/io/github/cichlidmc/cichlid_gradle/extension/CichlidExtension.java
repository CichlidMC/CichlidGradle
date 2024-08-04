package io.github.cichlidmc.cichlid_gradle.extension;

import io.github.cichlidmc.cichlid_gradle.run.RunConfiguration;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.plugins.ExtensionAware;

public interface CichlidExtension extends ExtensionAware {
	NamedDomainObjectContainer<RunConfiguration> getRuns();
}
