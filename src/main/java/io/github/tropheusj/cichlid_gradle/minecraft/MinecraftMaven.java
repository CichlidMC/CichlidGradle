package io.github.tropheusj.cichlid_gradle.minecraft;

import com.google.common.base.Suppliers;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Library;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest.Version;
import io.github.tropheusj.cichlid_gradle.util.FileUtils;
import io.github.tropheusj.cichlid_gradle.util.IoSupplier;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages a maven repository stored on the local filesystem containing Minecraft resources.
 * Intercepts requests to try to download missing versions.
 */
public class MinecraftMaven {
    public static final String PATH = "caches/cichlid-gradle/minecraft-maven";
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
     * Returns a path to the file at the given URI. The file will always exist.
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
        this.downloadSide(full, Side.SERVER);
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
                if (!Files.exists(this.lockFile)) {
                    // can't use open option, gradle breaks it
                    Files.createDirectories(this.root);
                    Files.createFile(this.lockFile);
                }

                try (FileChannel channel = FileChannel.open(this.lockFile, StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
                    return supplier.get();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void makePom(FullVersion version, String artifactName, Path file) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xml = builder.newDocument();

            Node project = xml.appendChild(xml.createElement("project"));

            Node groupId = project.appendChild(xml.createElement("groupId"));
            groupId.setTextContent("net.minecraft");

            Node artifactId = project.appendChild(xml.createElement("artifactId"));
            artifactId.setTextContent(artifactName);

            Node versionElement = project.appendChild(xml.createElement("version"));
            versionElement.setTextContent(version.id());

            Node dependencies = project.appendChild(xml.createElement("dependencies"));

            for (Library library : version.libraries()) {
                Node dependency = dependencies.appendChild(xml.createElement("dependency"));
                String[] split = library.name().split(":");

                Node libGroup = dependency.appendChild(xml.createElement("groupId"));
                libGroup.setTextContent(split[0]);

                Node libArtifact = dependency.appendChild(xml.createElement("artifactId"));
                libArtifact.setTextContent(split[1]);

                Node libVersion = dependency.appendChild(xml.createElement("version"));
                libVersion.setTextContent(split[2]);

                Node scope = dependency.appendChild(xml.createElement("scope"));
                scope.setTextContent("compile");
            }

            DOMSource source = new DOMSource(xml);
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute("indent-number", 4);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Files.createDirectories(file.getParent());
            try (OutputStream stream = Files.newOutputStream(file)) {
                transformer.transform(source, new StreamResult(stream));
            }
        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private static void makeMetadata(VersionManifest manifest, String artifactName, Path file) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xml = builder.newDocument();
            Node metadata = xml.appendChild(xml.createElement("metadata"));

            Node groupId = metadata.appendChild(xml.createElement("groupId"));
            groupId.setTextContent("net.minecraft");

            Node artifactId = metadata.appendChild(xml.createElement("artifactId"));
            artifactId.setTextContent(artifactName);

            Node versioning = metadata.appendChild(xml.createElement("versioning"));
            Node versions = versioning.appendChild(xml.createElement("versions"));

            for (VersionManifest.Version version : manifest.versions()) {
                Node element = versions.appendChild(xml.createElement("version"));
                element.setTextContent(version.id());
            }

            DOMSource source = new DOMSource(xml);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            Files.createDirectories(file.getParent());
            try (OutputStream stream = Files.newOutputStream(file)) {
                transformer.transform(source, new StreamResult(stream));
            }
        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
