package io.github.cichlidmc.cichlid_gradle.extension.repo;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.ExtensionAware;

import java.net.URI;

public class MinecraftReposExtension {
    private final Project project;

    private MinecraftReposExtension(Project project) {
        this.project = project;
    }

    public MavenArtifactRepository libraries() {
        return this.libraries(repo -> {});
    }

    public MavenArtifactRepository libraries(Action<? super MavenArtifactRepository> action) {
        return this.project.getRepositories().maven(repo -> {
            repo.setName("Minecraft Libraries");
            repo.setUrl(URI.create("https://libraries.minecraft.net/"));
            action.execute(repo);
        });
    }

    public void versions() {
        CichlidCache cache = CichlidCache.get(this.project);
        RepositoryHandler repos = this.project.getRepositories();

        repos.exclusiveContent(exclusive -> {
            exclusive.filter(contents -> {
                contents.includeGroup(CichlidCache.MINECRAFT_GROUP);
				CichlidCache.MINECRAFT_MODULES.forEach(
						module -> contents.includeModule(CichlidCache.MINECRAFT_GROUP, module)
				);
			});
            exclusive.forRepository(() -> repos.ivy(repo -> {
                repo.setName("Minecraft Versions");
                repo.setUrl(cache.root);
                repo.patternLayout(layout -> {
                    layout.ivy(CichlidCache.IVY_PATTERN);
                    layout.artifact(CichlidCache.IVY_PATTERN);
                });
			}));
        });
    }

    public static void setup(Project project) {
        MinecraftReposExtension mc = new MinecraftReposExtension(project);
        // I don't know why but ExtensionAware is added at runtime
        ((ExtensionAware) project.getRepositories()).getExtensions().add("minecraft", mc);
    }
}
