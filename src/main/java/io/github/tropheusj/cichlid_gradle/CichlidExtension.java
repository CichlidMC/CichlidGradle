package io.github.tropheusj.cichlid_gradle;

import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class CichlidExtension {
    @Inject
    protected abstract Project getProject();
}
