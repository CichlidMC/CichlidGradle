package io.github.cichlidmc.cichlid_gradle.util;

import java.util.Locale;

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
}
