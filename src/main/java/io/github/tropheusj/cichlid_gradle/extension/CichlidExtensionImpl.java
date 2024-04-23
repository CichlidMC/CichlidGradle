package io.github.tropheusj.cichlid_gradle.extension;

import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class CichlidExtensionImpl implements CichlidExtension {
    @Inject
    protected abstract Project getProject();
}
