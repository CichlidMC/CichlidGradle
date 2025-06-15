package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.distmarker.Dist;

import java.nio.file.Path;
import java.util.Locale;

public class JarsStorage {
    private final Path root;

    JarsStorage(Path root) {
        this.root = root;
    }

    public Path get(Dist dist) {
        String name = dist.name().toLowerCase(Locale.ROOT);
        return this.root.resolve(name + ".jar");
    }

    public Path get(Distribution dist) {
        return this.root.resolve(dist.name + ".jar");
    }

    public Path merged() {
        return this.root.resolve("merged.jar");
    }
}
