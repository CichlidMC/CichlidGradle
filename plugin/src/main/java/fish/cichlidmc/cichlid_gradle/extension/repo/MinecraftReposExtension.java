package fish.cichlidmc.cichlid_gradle.extension.repo;

import fish.cichlidmc.cichlid_gradle.cache.mcmaven.MinecraftMaven;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.ExtensionAware;

import java.net.URI;

public final class MinecraftReposExtension {
    public static final URI LIBRARIES_URL = URI.create("https://libraries.minecraft.net/");

    private final Project project;

    private MinecraftReposExtension(Project project) {
        this.project = project;
    }

    public MavenArtifactRepository libraries() {
        RepositoryHandler repos = this.project.getRepositories();

        MavenArtifactRepository maven = repos.maven(repo -> {
            repo.setName("Minecraft Libraries");
            repo.setUrl(LIBRARIES_URL);
        });

        repos.exclusiveContent(exclusive -> exclusive.forRepositories(maven).filter(contents -> {
            // LWJGL must be gotten from here instead of central
            contents.includeGroup("org.lwjgl");
            // this is the only place mojang libraries are hosted, might as well declare it
            contents.includeGroup("com.mojang");
        }));

        return maven;
    }

    public MavenArtifactRepository versions() {
        RepositoryHandler repos = this.project.getRepositories();
        URI url = URI.create(MinecraftMaven.createProtocol(this.project) + ":///");

        MavenArtifactRepository maven = repos.maven(repo -> {
            repo.setName("Minecraft Versions");
            repo.setUrl(url);
            repo.metadataSources(sources -> {
                sources.mavenPom();
                sources.ignoreGradleMetadataRedirection();
            });
        });

        repos.exclusiveContent(exclusive -> exclusive.forRepositories(maven).filter(contents -> {
            contents.includeGroup(MinecraftMaven.GROUP);
            contents.includeModule(MinecraftMaven.GROUP, MinecraftMaven.MODULE);
        }));

        return maven;
    }

    public static void setup(Project project) {
        MinecraftReposExtension mc = new MinecraftReposExtension(project);
        // I don't know why but ExtensionAware is added at runtime
        ((ExtensionAware) project.getRepositories()).getExtensions().add("minecraft", mc);
    }
}
