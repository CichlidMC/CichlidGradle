package io.github.tropheusj.cichlid_gradle.minecraft;

import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest;
import org.gradle.api.invocation.Gradle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class MinecraftMaven {
    public static final String PATH = "caches/cichlid-gradle/minecraft-maven";

    public static synchronized void ensureDummied(Gradle gradle) {
        Path path = getPath(gradle);
        Path manifest = path.resolve("version_manifest.json");
        VersionManifest cached = Files.exists(manifest) ? VersionManifest.ofFile(manifest) : null;
        VersionManifest latest = VersionManifest.fetch();
        if (Objects.equals(cached, latest)) {
            // nothing has changed
            return;
        }

        latest.save(manifest);

        for (Side side : Side.values()) {
            Path minecraft = path.resolve("net/minecraft/minecraft-" + side);
            makeMetadata(latest, "minecraft-" + side.name, minecraft.resolve("maven-metadata.xml"));

            for (VersionManifest.Version version : latest.versions()) {
                String name = version.id();
                Path dir = minecraft.resolve(name);
                Path jar = dir.resolve("minecraft-" + side + '-' + name + ".jar");
                if (Files.exists(jar))
                    continue;

                try {
                    Files.createDirectories(jar.getParent());
                    Files.createFile(jar);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void makeMetadata(VersionManifest manifest, String artifactName, Path file) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xml = builder.newDocument();
            Element metadata = xml.createElement("metadata");
            xml.appendChild(metadata);

            Element groupId = xml.createElement("groupId");
            groupId.setTextContent("net.minecraft");
            metadata.appendChild(groupId);

            Element artifactId = xml.createElement("artifactId");
            artifactId.setTextContent(artifactName);
            metadata.appendChild(artifactId);

            Element versioning = xml.createElement("versioning");
            metadata.appendChild(versioning);

            Element versions = xml.createElement("versions");
            versioning.appendChild(versions);

            for (VersionManifest.Version version : manifest.versions()) {
                Element element = xml.createElement("version");
                element.setTextContent(version.id());
                versions.appendChild(element);
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

    public static Path getPath(Gradle gradle) {
        Path home = gradle.getGradleUserHomeDir().toPath();
        return home.resolve(PATH);
    }

    public static URI getUri(Gradle gradle) {
        return getPath(gradle).toUri();
    }
}
