package io.github.cichlidmc.cichlid_gradle.cache;

import com.google.common.base.Suppliers;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Artifact;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Features;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Library;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Natives;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Rule;
import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.VersionManifest;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.VersionManifest.Version;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.cichlid_gradle.util.IoRunnable;
import io.github.cichlidmc.cichlid_gradle.util.XmlBuilder;
import io.github.cichlidmc.cichlid_gradle.util.XmlBuilder.XmlElement;
import net.neoforged.art.api.Renamer;
import net.neoforged.art.api.Transformer;
import net.neoforged.srgutils.IMappingFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Manages a maven repository stored on the local filesystem containing Minecraft resources.
 */
public class MinecraftMaven {
    public static final int FORMAT = 1;
    public static final String PATH = "maven/v" + FORMAT;

    private static final Logger logger = Logging.getLogger(MinecraftMaven.class);
    private static final Supplier<VersionManifest> manifest = Suppliers.memoize(VersionManifest::fetch);
    private static final Object lock = new Object();

    public final Path root;

    private final Path lockFile;
    private final CichlidCache cache;

    MinecraftMaven(Path root, CichlidCache cache) {
        this.root = root;
        this.lockFile = root.resolve(".lock");
        this.cache = cache;
    }

    public void ensureVersionDownloaded(String version) {
        this.doLocked(() -> {
            // all versions have a client, check that
            Path versionDir = this.module("minecraft-client", version);
            if (!Files.exists(versionDir)) {
                this.tryDownloadVersion(version);
            }
        });
    }

    private void tryDownloadVersion(String version) throws IOException {
        // doesn't exist, download everything
        VersionManifest manifest = MinecraftMaven.manifest.get();
        Map<String, Version> versions = manifest.mapVersions();
        if (!versions.containsKey(version))
            return; // fake version
        logger.quiet("Minecraft {} not cached, downloading for the first time. This could take a while.", version);
        this.downloadVersion(versions.get(version));
        logger.quiet("Download of Minecraft {} complete.", version);
    }

    private void downloadVersion(Version version) throws IOException {
        FullVersion full = version.expand();

        // all versions have a client
        this.downloadDist(full, Distribution.CLIENT);
        // not all versions have a server
        if (full.downloads().server().isPresent()) {
            this.downloadDist(full, Distribution.SERVER);
            this.generateMergedJar(full);
            // bundler is handled in downloadDist
        }

        this.cache.assets.downloadAssets(full);
        this.cache.natives.extractNatives(full);
        this.cache.runs.generateRuns(full, this);
    }

    private void downloadDist(FullVersion version, Distribution dist) throws IOException {
        logger.quiet("Downloading {}", dist);
        if (dist.isSpecial()) {
            throw new IllegalArgumentException();
        }

        String name = "minecraft-" + dist;
        Path destFile = this.artifact(name, version.id(), "jar");

        FullVersion.Downloads downloads = version.downloads();
        FullVersion.Download jarDownload = choose(dist, downloads::client, downloads.server()::get);

        Path mappingsFile = this.downloadMappings(version, dist);
        if (mappingsFile != null) {
            logger.quiet("Remapping to mojmap");
            // download to a temp file first for remapping
            Path temp = this.artifact(name, version.id(), "jar.tmp");
            FileUtils.download(jarDownload, temp);

            if (dist == Distribution.SERVER) {
                this.handleBundler(version, temp);
            }

            // remap
            List<String> log = new ArrayList<>();
            // mojmap is distributed named -> obf, reverse it
            IMappingFile mappings = IMappingFile.load(mappingsFile.toFile()).reverse();
            Transformer.Factory transformer = Transformer.renamerFactory(mappings, true);
            try (Renamer renamer = Renamer.builder().logger(log::add).add(transformer).build()) {
                renamer.run(temp.toFile(), destFile.toFile());
            }
            Path logFile = temp.resolveSibling("remap_log.txt");
            Files.writeString(logFile, String.join("\n", log));
            Files.delete(temp);
            FileUtils.removeJarSignatures(destFile);
            logger.quiet("Remapping done.");
        } else {
            logger.quiet("No mojmap for this version, skipping remapping.");
            // just download the jar
            FileUtils.download(jarDownload, destFile);
        }

        // TODO: decompile to generate sources

        // generate a POM
        logger.quiet("Generating POM");
        Path pomFile = this.artifact(name, version.id(), "pom");
        this.makePom(version, name, pomFile);
        logger.quiet("Finished downloading {}.", dist);
    }

    private void generateMergedJar(FullVersion version) {
        // TODO
    }

    private void handleBundler(FullVersion version, Path serverTempJar) throws IOException {
        try (JarFile jarFile = new JarFile(serverTempJar.toFile())) {
            String format = jarFile.getManifest().getMainAttributes().getValue("Bundler-Format");
            if (format == null)
                return;

            logger.quiet("Un-bundling server");
            if (!format.equals("1.0")) {
                logger.warn("Server bundle uses an untested format, this may not go well.");
            }
        }

        // move bundler to it's correct location
        Path bundler = this.artifact("minecraft-bundler", version.id(), "jar");
        Files.createDirectories(bundler.getParent());
        Files.move(serverTempJar, bundler);
        // generate POM
        Path pomFile = this.artifact("minecraft-bundler", version.id(), "pom");
        XmlBuilder.create().add(new XmlElement("project", List.of(
                new XmlElement("groupId", "net.minecraft"),
                new XmlElement("artifactId", "minecraft-bundler"),
                new XmlElement("version", version.id())
        ))).write(pomFile);

        // locate and extract server
        try (FileSystem fs = FileSystems.newFileSystem(bundler)) {
            Path versions = fs.getPath("META-INF", "versions");
            Path versionDir = FileUtils.getSingleFileInDirectory(versions);
            if (versionDir == null)
                return;
            Path realServer = FileUtils.getSingleFileInDirectory(versionDir);
            if (realServer == null)
                return;

            // copy server to its correct location
            Files.copy(realServer, serverTempJar);
            logger.quiet("Server un-bundled.");
        }
    }

    @Nullable
    private Path downloadMappings(FullVersion version, Distribution side) {
        FullVersion.Downloads downloads = version.downloads();
        Optional<FullVersion.Download> optional = choose(side, downloads::clientMappings, downloads::serverMappings);
        if (optional.isPresent()) {
            Path file = this.artifact(side + "-mappings", version.id(), "txt");
            FileUtils.download(optional.get(), file);
            return file;
        }
        // No mojmap for this version.
        return null;
    }

    private void doLocked(IoRunnable runnable) {
        synchronized (lock) {
            try {
                FileUtils.ensureCreated(this.lockFile);
                try (FileChannel channel = FileChannel.open(this.lockFile, StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
                    runnable.run();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Path to a module under net/minecraft, ex. net/minecraft/{name}/{version}
     */
    public Path module(String name, String version) {
        return this.root.resolve("net")
                .resolve("minecraft")
                .resolve(name)
                .resolve(version);
    }

    /**
     * Path to an artifact, located at net/minecraft/{name}/{version}/{name}-{version}.{extension}
     */
    public Path artifact(String name, String version, String extension) {
        String filename = name + '-' + version + '.' + extension;
        return this.module(name, version).resolve(filename);
    }

    /**
     * Path to an artifact, located at net/minecraft/{name}/{version}/{name}-{version}-{classifier}.{extension}
     */
    public Path artifact(String name, String version, String classifier, String extension) {
        String filename = name + '-' + version + '-' + classifier + '.' + extension;
        return this.module(name, version).resolve(filename);
    }

    private void makePom(FullVersion version, String artifactName, Path file) throws IOException {
        XmlBuilder.create().add(new XmlElement("project", List.of(
                new XmlElement("groupId", "net.minecraft"),
                new XmlElement("artifactId", artifactName),
                new XmlElement("version", version.id()),
                new XmlElement("dependencies", version.libraries().stream().flatMap(this::makeLibraryPoms).toList())
        ))).write(file);
    }

    private Stream<XmlElement> makeLibraryPoms(Library library) {
        // check rules first
        if (!Rule.test(library.rules(), Features.EMPTY))
            return Stream.empty();

        List<XmlElement> elements = new ArrayList<>();
        Optional<Artifact> artifact = library.download().artifact();
        if (artifact.isPresent()) {
            elements.add(makeDependencyXml(library.name()));
        }

        Optional<Natives> natives = library.natives();
        if (natives.isPresent()) {
            String classifier = natives.get().choose();
            if (classifier != null) {
                String notation = library.name() + ':' + classifier;
                elements.add(makeDependencyXml(notation));
            }
        }

        if (elements.isEmpty()) {
            throw new IllegalStateException("Library has nothing to download: " + library);
        }

        return elements.stream();
    }

    private static XmlElement makeDependencyXml(String notation) {
        String[] split = notation.split(":");
        XmlElement element = new XmlElement("dependency", new ArrayList<>(List.of(
                new XmlElement("groupId", split[0]),
                new XmlElement("artifactId", split[1]),
                new XmlElement("version", split[2]),
                new XmlElement("scope", "compile")
        )));
        if (split.length > 3) {
            element.children().add(new XmlElement("classifier", split[3]));
        }
        return element;
    }

    private static <T> T choose(Distribution side, Supplier<T> client, Supplier<T> server) {
        return switch (side) {
            case CLIENT -> client.get();
            case SERVER -> server.get();
            case MERGED, BUNDLER -> throw new IllegalArgumentException();
        };
    }
}
