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

    public Path temp(Distribution distribution) {
        return this.path(distribution, "tmp");
    }

    public Path metadata(Distribution distribution) {
        return this.path(distribution, "xml");
    }

    private Path path(Distribution distribution, String extension) {
        return this.root.resolve("minecraft-" + distribution + '-' + this.version + '.' + extension);
    }
}
