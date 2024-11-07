package io.github.cichlidmc.cichlid_gradle.extension.repo;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;

import java.net.URI;

public class CichlidReposExtension {
    private final Project project;

    private CichlidReposExtension(Project project) {
        this.project = project;
    }

    public void releases() {
        this.project.getRepositories().maven(repo -> {
            repo.setName("Cichlid Releases");
            repo.setUrl(URI.create("https://mvn.devos.one/releases"));
        });
    }

    public void snapshots() {
        this.project.getRepositories().maven(repo -> {
            repo.setName("Cichlid Snapshots");
            repo.setUrl(URI.create("https://mvn.devos.one/snapshots"));
        });
    }

    public static void setup(Project project) {
        CichlidReposExtension mc = new CichlidReposExtension(project);
        // I don't know why but ExtensionAware is added at runtime
        ((ExtensionAware) project.getRepositories()).getExtensions().add("cichlid", mc);
    }
}
