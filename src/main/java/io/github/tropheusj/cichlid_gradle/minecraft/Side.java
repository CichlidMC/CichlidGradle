package io.github.tropheusj.cichlid_gradle.minecraft;

import java.util.Locale;

public enum Side {
    CLIENT, SERVER, MERGED;

    public final String name = this.name().toLowerCase(Locale.ROOT);

    @Override
    public String toString() {
        return this.name;
    }
}
