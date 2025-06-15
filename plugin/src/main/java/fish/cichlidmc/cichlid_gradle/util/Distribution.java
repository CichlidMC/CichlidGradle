package fish.cichlidmc.cichlid_gradle.util;

import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.DecompileTask;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.MergeTask;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.SetupTask;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public enum Distribution {
    CLIENT, SERVER, MERGED;

    public final String name = this.name().toLowerCase(Locale.ROOT);

    @Override
    public String toString() {
        return this.name;
    }

    public boolean needsAssets() {
        return this == CLIENT || this == MERGED;
    }

    public <T> T choose(T client, T server) {
        return switch (this) {
            case CLIENT -> client;
            case SERVER -> server;
            case MERGED -> throw new IllegalStateException("Cannot call choose(client, server) on MERGED");
        };
    }

    public <T> T choose(Supplier<T> client, Supplier<T> server) {
        return this.<Supplier<T>>choose(client, server).get();
    }

    public CacheTask createSetupTask(CacheTaskEnvironment env) {
        return this == MERGED ? new MergeTask(env) : new SetupTask(env);
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
