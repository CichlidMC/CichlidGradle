package io.github.tropheusj.cichlid_gradle.minecraft;

import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Artifact;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Library;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.VersionManifest.Version;
import org.gradle.api.invocation.Gradle;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
            String artifactName = "minecraft-" + side;
            Path minecraft = path.resolve("net/minecraft/" + artifactName);

            makeMetadata(latest, artifactName, minecraft.resolve("maven-metadata.xml"));

            for (VersionManifest.Version version : latest.versions()) {
                String versionName = version.id();
                Path dir = minecraft.resolve(versionName);
                Path jar = dir.resolve(artifactName + '-' + versionName + ".jar");
                if (Files.exists(jar))
                    continue;
                Path pom = dir.resolve(artifactName + '-' + versionName + ".pom");

                try {
                    Files.createDirectories(jar.getParent());
                    Files.createFile(jar);
                    makePom(version, artifactName, pom);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void makePom(Version version, String artifactName, Path file) {
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

            FullVersion fullVersion = version.expand();
            for (Library library : fullVersion.libraries()) {
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
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

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

    public static Path getPath(Gradle gradle) {
        Path home = gradle.getGradleUserHomeDir().toPath();
        return home.resolve(PATH);
    }

    public static URI getUri(Gradle gradle) {
        return getPath(gradle).toUri();
    }
}
