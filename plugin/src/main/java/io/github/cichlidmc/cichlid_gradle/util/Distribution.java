package io.github.cichlidmc.cichlid_gradle.util;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public enum Distribution {
    CLIENT, SERVER,
    /**
     * Merged client + server jar
     */
    MERGED,
    /**
     * Modern server jars are bundled into a different jar alongside libraries. This is that jar.
     */
    BUNDLER;

    public final String name = this.name().toLowerCase(Locale.ROOT);

    @Override
    public String toString() {
        return this.name;
    }

    public boolean isSpecial() {
        return this == MERGED || this == BUNDLER;
    }

    public <T> T choose(T client, T server) {
        return this.choose(() -> client, () -> server);
    }

    public <T> T choose(Supplier<T> client, Supplier<T> server) {
        return switch (this) {
            case CLIENT -> client.get();
            case SERVER -> server.get();
            case MERGED, BUNDLER -> throw new IllegalStateException();
        };
    }

    public static Optional<Distribution> ofName(String name) {
        for (Distribution dist : values()) {
            if (dist.name.equals(name)) {
                return Optional.of(dist);
            }
        }

        return Optional.empty();
    }
}
