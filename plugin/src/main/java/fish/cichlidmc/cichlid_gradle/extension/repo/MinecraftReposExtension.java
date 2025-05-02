package fish.cichlidmc.cichlid_gradle.extension.repo;

import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.cache.mcmaven.MinecraftMaven;
import org.gradle.api.Action;
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
        return this.libraries(repo -> {});
    }

    public MavenArtifactRepository libraries(Action<? super MavenArtifactRepository> action) {
        RepositoryHandler repos = this.project.getRepositories();

        MavenArtifactRepository libraries = repos.maven(repo -> {
            repo.setName("Minecraft Libraries");
            repo.setUrl(LIBRARIES_URL);
            action.execute(repo);
        });

        repos.exclusiveContent(exclusive -> exclusive.forRepositories(libraries).filter(contents -> {
            // LWJGL must be gotten from here instead of central
            contents.includeGroup("org.lwjgl");
            // this is the only place mojang libraries are hosted, might as well declare it
            contents.includeGroup("com.mojang");
        }));

        return libraries;
    }

    public void versions() {
        RepositoryHandler repos = this.project.getRepositories();

        repos.exclusiveContent(exclusive -> {
            exclusive.filter(contents -> {
                contents.includeGroup(CichlidCache.MINECRAFT_GROUP);
				CichlidCache.MINECRAFT_MODULES.forEach(
						module -> contents.includeModule(CichlidCache.MINECRAFT_GROUP, module)
				);
			});

            exclusive.forRepository(() -> repos.maven(repo -> {
                repo.setName("Minecraft Versions");
                repo.setUrl(MinecraftMaven.ROOT);
                repo.metadataSources(sources -> {
                    sources.mavenPom();
                    sources.ignoreGradleMetadataRedirection();
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
