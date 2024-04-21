package io.github.tropheusj.cichlid_gradle.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest;
import io.github.tropheusj.cichlid_gradle.util.Downloadable;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

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

    public Path getJar(String version, Side side) {
        return this.getLocked(() -> {
            Path file = this.jars.resolve(JAR_PATTERN.formatted(version, side));
            if (!Files.exists(file)) {
                logger.quiet("Downloading Minecraft {} as it is not cached yet.", version);
                // download if it exists, throws otherwise
                this.tryDownload(version);
            }
            return file;
        });
    }

    private void tryDownload(String versionString) throws IOException {
        VersionManifest manifest = VersionManifest.fetch();
        VersionManifest.Version version = manifest.mapVersions().get(versionString);
        if (version == null) {
            throw new RuntimeException("Minecraft version does not exist: " + versionString);
        }

        FullVersion full = version.expand();

        // four primary files
        FullVersion.Downloads downloads = full.downloads();
        this.downloadSidedFile(this.jars, JAR_PATTERN, versionString, Side.CLIENT, downloads.client());
        this.downloadSidedFile(this.jars, JAR_PATTERN, versionString, Side.SERVER, downloads.server());
        this.downloadSidedFile(this.jars, MAPPINGS_PATTERN, versionString, Side.CLIENT, downloads.clientMappings());
        this.downloadSidedFile(this.jars, MAPPINGS_PATTERN, versionString, Side.SERVER, downloads.serverMappings());

        // all libraries
        Path libs = this.libraries.resolve(versionString);
        for (FullVersion.Library library : full.libraries()) {
            FullVersion.Artifact artifact = library.download().artifact();
            String path = artifact.path();
            this.downloadFile(libs.resolve(path), artifact);
        }
    }

    private void downloadSidedFile(Path destDir, String pattern, String version, Side side, FullVersion.Download download) throws IOException {
        this.downloadFile(destDir.resolve(pattern.formatted(version, side)), download);
    }

    private void downloadFile(Path dest, Downloadable download) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(download.url()).build();
        try {
            logger.lifecycle("Downloading {}...", dest.getFileName());
            long start = System.currentTimeMillis();
            byte[] data = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
            long end = System.currentTimeMillis();
            int mb = data.length / 1000 / 1000;
            logger.lifecycle("Downloaded {}mb in {}ms.", mb, end - start);
            // validate
            if (download.size() != data.length) {
                throw new RuntimeException("Downloaded file did not match expected size of " + download.size());
            }
            if (!sha1(data).equals(download.sha1())) {
                throw new RuntimeException("Downloaded file did not match expected hash of " + download.sha1());
            }
            // valid
            Files.createDirectories(dest.getParent());
            Files.createFile(dest);
            Files.write(dest, data);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> void writeWithCodec(Path path, Codec<T> codec, T obj) throws IOException {
        JsonElement json = codec.encodeStart(JsonOps.INSTANCE, obj).getOrThrow();
        Files.createFile(path);
        Files.writeString(path, gson.toJson(json));
    }

    private void runLocked(IoRunnable runnable) {
        synchronized (lock) {
            try (FileChannel channel = FileChannel.open(this.lockFile, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                runnable.run();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private <T> T getLocked(IoSupplier<T> supplier) {
        var holder = new Object() { T value; };
        this.runLocked(() -> holder.value = supplier.get());
        return holder.value;
    }

    private static String sha1(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(data);
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    public static GlobalMinecraftCache get(Path dir) {
        return new GlobalMinecraftCache(dir.resolve(DIR));
    }

    public interface IoRunnable {
        void run() throws IOException;
    }

    public interface IoSupplier<T> {
        T get() throws IOException;
    }
}
