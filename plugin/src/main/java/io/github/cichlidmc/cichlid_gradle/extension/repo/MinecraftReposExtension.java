package io.github.cichlidmc.cichlid_gradle.extension.repo;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;

import java.net.URI;

public class MinecraftReposExtension {
    private final Project project;

    private MinecraftReposExtension(Project project) {
        this.project = project;
    }

    public void libraries() {
        this.project.getRepositories().maven(repo -> {
            repo.setName("Minecraft Libraries");
            repo.setUrl(URI.create("https://libraries.minecraft.net/"));
        });
    }

    public void pistonMeta() {
        CichlidCache cache = CichlidCache.get(this.project);
        this.project.getRepositories().maven(repo -> {
            repo.setName("Piston Meta");
            repo.setUrl(cache.maven.root);
        });
    }

    public static void setup(Project project) {
        MinecraftReposExtension mc = new MinecraftReposExtension(project);
        // I don't know why but ExtensionAware is added at runtime
        ((ExtensionAware) project.getRepositories()).getExtensions().add("minecraft", mc);
    }
}
