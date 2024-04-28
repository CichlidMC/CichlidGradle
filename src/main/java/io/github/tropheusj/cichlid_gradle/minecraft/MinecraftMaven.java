package io.github.tropheusj.cichlid_gradle.minecraft;

import com.google.common.base.Suppliers;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Artifact;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Classifiers;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Features;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Library;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Natives;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Rule;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest.Version;
import io.github.tropheusj.cichlid_gradle.util.FileUtils;
import io.github.tropheusj.cichlid_gradle.util.IoSupplier;
import io.github.tropheusj.cichlid_gradle.util.XmlBuilder;
import io.github.tropheusj.cichlid_gradle.util.XmlBuilder.XmlElement;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public MinecraftMaven(Path root) {
        this.root = root;
        this.lockFile = root.resolve(".lock");
    }

    public static MinecraftMaven get(Path path) {
        return new MinecraftMaven(path.resolve(PATH));
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
            Path versionDir = this.root.resolve("net/minecraft/minecraft-client").resolve(version);
            if (Files.exists(versionDir)) {
                // version has already been downloaded, URI is just invalid
                return null;
            }
            this.tryDownloadVersion(version);
            return Files.exists(file) ? file : null;
        });
    }

    private void tryDownloadVersion(String version) {
        // doesn't exist, download everything
        VersionManifest manifest = MinecraftMaven.manifest.get();
        Map<String, Version> versions = manifest.mapVersions();
        if (!versions.containsKey(version))
            return; // fake version
        logger.quiet("Minecraft {} not cached, downloading...", version);
        this.downloadVersion(versions.get(version));
        logger.quiet("Download complete.");
    }

    private void downloadVersion(Version version) {
        FullVersion full = version.expand();

        this.downloadSide(full, Side.CLIENT);
//        this.downloadSide(full, Side.SERVER);
    }

    private void downloadSide(FullVersion version, Side side) {
        // download the jar
        Path dir = this.root.resolve("net/minecraft/minecraft-" + side).resolve(version.id());
        String archiveName = "minecraft-" + side;
        String filename = archiveName + '-' + version.id();
        Path dest = dir.resolve(filename + ".jar");
        FileUtils.download(version.downloads().jar(side), dest);
        // generate a POM
        this.makePom(version, archiveName, dir.resolve(filename + ".pom"));
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
}
