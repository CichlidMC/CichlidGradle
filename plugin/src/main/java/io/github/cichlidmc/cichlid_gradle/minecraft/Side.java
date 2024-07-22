package io.github.cichlidmc.cichlid_gradle.minecraft;

import java.util.Locale;

public enum Side {
    CLIENT, SERVER, MERGED;

    public final String name = this.name().toLowerCase(Locale.ROOT);

    @Override
    public String toString() {
        return this.name;
    }

    public static Side of(String lowercase) {
        return switch (lowercase) {
            case "client" -> CLIENT;
            case "server" -> SERVER;
            case "merged" -> MERGED;
            default -> throw new IllegalArgumentException();
        };
    }
}
