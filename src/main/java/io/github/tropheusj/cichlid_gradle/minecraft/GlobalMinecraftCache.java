package io.github.tropheusj.cichlid_gradle.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Rule;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest;
import io.github.tropheusj.cichlid_gradle.util.Downloadable;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Global cache for unmodified Minecraft jars and mappings.
 * Location: <user>/.gradle/caches/cichlid-gradle/minecraft
 * Layout:
 * root
 * - .lock
 * - jars
 *      - <version>-<side>.jar
 * - mappings
 *      - <version>-<side>.txt
 * - libraries
 *      - <version>
 *          - library jars
 * Side can be either client, server, or merged.
 */
public class GlobalMinecraftCache {
    public static final String DIR = "caches/cichlid-gradle/minecraft";
    public static final String JAR_PATTERN = "%s-%s.jar";
    public static final String MAPPINGS_PATTERN = "%s-%s.txt";
    private static final Logger logger = Logging.getLogger(GlobalMinecraftCache.class);

    private static final HttpClient client = HttpClient.newBuilder().build();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Object lock = new Object();

    private final Path root;
    private final Path jars;
    private final Path mappings;
    private final Path libraries;
    private final Path lockFile;

    public GlobalMinecraftCache(Path root) {
        this.root = root;
        this.jars = root.resolve("jars");
        this.mappings = root.resolve("mappings");
        this.libraries = root.resolve("libraries");
        this.lockFile = root.resolve(".lock");
    }

    private <T> void writeWithCodec(Path path, Codec<T> codec, T obj) throws IOException {
        JsonElement json = codec.encodeStart(JsonOps.INSTANCE, obj).getOrThrow();
        Files.createFile(path);
        Files.writeString(path, gson.toJson(json));
    }

}
