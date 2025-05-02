package fish.cichlidmc.cichlid_gradle.extension.repo;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;

import java.net.URI;

public final class CichlidReposExtension {
    // everything Cichlid is currently hosted on the devOS maven.
    public static final URI DEVOS_RELEASES = URI.create("https://mvn.devos.one/releases");
    public static final URI DEVOS_SNAPSHOTS = URI.create("https://mvn.devos.one/snapshots");

    private final Project project;

    private CichlidReposExtension(Project project) {
        this.project = project;
    }

    public void releases() {
        this.project.getRepositories().maven(repo -> {
            repo.setName("Cichlid Releases");
            repo.setUrl(DEVOS_RELEASES);
            repo.content(content -> content.includeGroupAndSubgroups("fish.cichlidmc"));
        });
    }

    public void snapshots() {
        this.project.getRepositories().maven(repo -> {
            repo.setName("Cichlid Snapshots");
            repo.setUrl(DEVOS_SNAPSHOTS);
            repo.content(content -> content.includeGroupAndSubgroups("fish.cichlidmc"));
        });
    }

    public static void setup(Project project) {
        CichlidReposExtension mc = new CichlidReposExtension(project);
        // I don't know why but ExtensionAware is added at runtime
        ((ExtensionAware) project.getRepositories()).getExtensions().add("cichlid", mc);
    }
}
