package io.github.cichlidmc.cichlid_gradle.cache.storage;

import io.github.cichlidmc.cichlid_gradle.util.Distribution;

import java.nio.file.Path;

public class JarsStorage {
    private final Path root;
    private final String version;

    JarsStorage(Path root, String version) {
        this.root = root;
        this.version = version;
    }

    public Path path(Distribution distribution) {
        return this.path(distribution, "jar");
    }

    public Path sources(Distribution distribution) {
        return this.path(distribution, "sources", "jar");
    }

    public Path temp(Distribution distribution) {
        return this.path(distribution, "tmp");
    }

    public Path metadata(Distribution distribution) {
        return this.path(distribution, "pom");
    }

    private Path path(Distribution distribution, String extension) {
        return this.path(distribution, "", extension);
    }

    private Path path(Distribution distribution, String classifier, String extension) {
        String dashed = classifier.isBlank() ? "" : "-" + classifier;
        return this.root.resolve("minecraft-" + distribution + '-' + this.version + dashed + '.' + extension);
    }
}
