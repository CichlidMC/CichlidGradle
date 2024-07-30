package io.github.cichlidmc.cichlid_gradle.minecraft;

import com.google.common.base.Suppliers;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Artifact;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Features;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Library;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Natives;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion.Rule;
import io.github.cichlidmc.cichlid_gradle.util.DirDeleter;
import io.github.cichlidmc.cichlid_gradle.util.Side;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.VersionManifest;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.VersionManifest.Version;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.cichlid_gradle.util.IoSupplier;
import io.github.cichlidmc.cichlid_gradle.util.XmlBuilder;
import io.github.cichlidmc.cichlid_gradle.util.XmlBuilder.XmlElement;
import net.neoforged.art.api.Renamer;
import net.neoforged.art.api.Transformer;
import net.neoforged.srgutils.IMappingFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Manages a maven repository stored on the local filesystem containing Minecraft resources.
 * Intercepts requests to try to download missing versions.
 */
public class MinecraftMaven {
    public static final int FORMAT = 1;
    public static final String PATH = "caches/cichlid-gradle/minecraft-maven/v" + FORMAT;
    public static final Pattern MC = Pattern.compile("/net/minecraft/minecraft-(client|server|merged)/(.+)/minecraft-(client|server|merged)-(.+)\\.(pom|jar)");

    private static final Logger logger = Logging.getLogger(MinecraftMaven.class);
    private static final Supplier<VersionManifest> manifest = Suppliers.memoize(VersionManifest::fetch);
    private static final Object lock = new Object();

    private final Path root;
    private final Path lockFile;
    private final AssetStorage assetStorage;

    public MinecraftMaven(Path root, AssetStorage assetStorage) {
        this.root = root;
        this.lockFile = root.resolve(".lock");
        this.assetStorage = assetStorage;
    }

    public static MinecraftMaven get(Path path) {
        return new MinecraftMaven(path.resolve(PATH), AssetStorage.get(path));
    }

    /**
     * Returns a path to the file at the given URI. The file will always exist if non-null.
     */
    @Nullable
    public Path getFile(URI uri) {
        String path = uri.getPath();
        Matcher matcher = MC.matcher(path);
        if (!matcher.matches())
            return null;

        return this.getLocked(() -> {
            Path file = this.root.resolve(path.substring(1)); // cut off first slash
            if (Files.exists(file))
                return file;

            // doesn't exist, try to download
            String version = matcher.group(2);
            Path versionDir = this.module("minecraft-client", version);
            if (Files.exists(versionDir)) {
                // version has already been downloaded, URI is just invalid
                return null;
            }
            this.tryDownloadVersion(version);
            return Files.exists(file) ? file : null;
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
        this.downloadSide(full, Side.CLIENT);
        // not all versions have a server
        if (full.downloads().server().isPresent()) {
            this.downloadSide(full, Side.SERVER);
        }

        this.assetStorage.downloadAssets(full);
    }

    private void downloadSide(FullVersion version, Side side) throws IOException {
        logger.quiet("Downloading {}", side);
        if (side == Side.MERGED) {
            throw new IllegalArgumentException();
        }

        String name = "minecraft-" + side;
        Path destFile = this.artifact(name, version.id(), "jar");

        FullVersion.Downloads downloads = version.downloads();
        FullVersion.Download jarDownload = choose(side, downloads::client, downloads.server()::get);

        Path mappingsFile = this.downloadMappings(version, side);
        if (mappingsFile != null) {
            logger.quiet("Remapping to mojmap");
            // download to a temp file first for remapping
            Path temp = this.artifact(name, version.id(), "jar.tmp");
            FileUtils.download(jarDownload, temp);
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
            // remove signatures
            try (FileSystem fs = FileSystems.newFileSystem(destFile)) {
                Path metaInf = fs.getPath("META-INF");
                DirDeleter.run(metaInf);
            }
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
        logger.quiet("Finished downloading {}.", side);
    }

    @Nullable
    private Path downloadMappings(FullVersion version, Side side) {
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

    private <T> T getLocked(IoSupplier<T> supplier) {
        synchronized (lock) {
            try {
                FileUtils.ensureCreated(this.lockFile);
                try (FileChannel channel = FileChannel.open(this.lockFile, StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
                    return supplier.get();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Path to a module under net/minecraft, ex. net/minecraft/{name}/{version}
     */
    private Path module(String name, String version) {
        return this.root.resolve("net")
                .resolve("minecraft")
                .resolve(name)
                .resolve(version);
    }

    /**
     * Path to an artifact, located at net/minecraft/{name}/{version}/{name}-{version}.{extension}
     */
    private Path artifact(String name, String version, String extension) {
        String filename = name + '-' + version + '.' + extension;
        return this.module(name, version).resolve(filename);
    }

    /**
     * Path to an artifact, located at net/minecraft/{name}/{version}/{name}-{version}-{classifier}.{extension}
     */
    private Path artifact(String name, String version, String classifier, String extension) {
        String filename = name + '-' + version + '-' + classifier + '.' + extension;
        return this.module(name, version).resolve(filename);
    }

    private void makePom(FullVersion version, String artifactName, Path file) {
        XmlBuilder pom = XmlBuilder.create().add(new XmlElement("project", List.of(
                new XmlElement("groupId", "net.minecraft"),
                new XmlElement("artifactId", artifactName),
                new XmlElement("version", version.id()),
                new XmlElement("dependencies", version.libraries().stream().flatMap(this::makeLibraryPoms).toList())
        )));

        try {
            Files.createDirectories(file.getParent());
            try (OutputStream stream = Files.newOutputStream(file)) {
                pom.write(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<XmlElement> makeLibraryPoms(Library library) {
        // check rules first
        List<Rule> rules = library.rules();
        for (Rule rule : rules) {
            if (!rule.test(Features.EMPTY)) {
                return Stream.empty();
            }
        }

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

    private static <T> T choose(Side side, Supplier<T> client, Supplier<T> server) {
        return switch (side) {
            case CLIENT -> client.get();
            case SERVER -> server.get();
            case MERGED -> throw new IllegalArgumentException();
        };
    }
}
