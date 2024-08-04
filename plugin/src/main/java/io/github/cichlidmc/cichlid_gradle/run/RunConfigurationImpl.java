package io.github.cichlidmc.cichlid_gradle.run;

import javax.inject.Inject;

import org.gradle.api.NamedDomainObjectContainer;

public abstract class RunConfigurationImpl implements RunConfiguration {
	private NamedDomainObjectContainer<RunConfiguration> container;

	@Inject
	public RunConfigurationImpl() {
	}

	public void setContainer(NamedDomainObjectContainer<RunConfiguration> container) {
		this.container = container;
	}

	@Override
	public void copyFrom(String other) {
		RunConfiguration parent = this.container.getByName(other);
		this.getMainClass().set(parent.getMainClass());
		this.getProgramArgs().set(parent.getProgramArgs());
		this.getJvmArgs().set(parent.getJvmArgs());
	}
}
