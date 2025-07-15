package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.XmlBuilder;
import fish.cichlidmc.cichlid_gradle.util.io.WorkFile;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.rule.Features;
import fish.cichlidmc.pistonmetaparser.rule.Rule;
import fish.cichlidmc.pistonmetaparser.version.library.Artifact;
import fish.cichlidmc.pistonmetaparser.version.library.Classifier;
import fish.cichlidmc.pistonmetaparser.version.library.Library;
import fish.cichlidmc.pistonmetaparser.version.library.Natives;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PomGenerator {
	public static final String VERSION_PLACEHOLDER = "${version}";

	public static void tryGenerate(FullVersion version, Distribution dist, Path output) throws IOException {
		WorkFile.doIfEmpty(output, file -> {
			OutputStream stream = Channels.newOutputStream(file.channel);
			XmlBuilder.create().add(new XmlBuilder.XmlElement("project", List.of(
					new XmlBuilder.XmlElement("modelVersion", "4.0.0"),
					new XmlBuilder.XmlElement("groupId", "net.minecraft"),
					new XmlBuilder.XmlElement("artifactId", "minecraft-" + dist.name),
					new XmlBuilder.XmlElement("version", VERSION_PLACEHOLDER),
					new XmlBuilder.XmlElement("dependencies", makeDependencyElements(version, dist))
			))).write(stream);
		});
	}

	private static List<XmlBuilder.XmlElement> makeDependencyElements(FullVersion version, Distribution dist) {
		List<XmlBuilder.XmlElement> elements = version.libraries.stream()
				.flatMap(PomGenerator::makeDependencyElements)
				// explicitly collect to a mutable list
				.collect(Collectors.toCollection(ArrayList::new));

		// when merged, add a dependency on distmarker
		if (dist == Distribution.MERGED) {
			// I don't mind hardcoding this version here because it will hopefully never be updated again
			elements.add(makeDependencyElement("fish.cichlidmc", "distribution-marker", "1.0.1", null));
		}

		return elements;
	}

	private static Stream<XmlBuilder.XmlElement> makeDependencyElements(Library library) {
		// check rules first
		if (!Rule.test(library.rules, Features.EMPTY))
			return Stream.empty();

		List<XmlBuilder.XmlElement> elements = new ArrayList<>();
		Optional<Artifact> artifact = library.artifact;
		if (artifact.isPresent()) {
			elements.add(makeDependencyElement(library.name));
		}

		Optional<Classifier> classifier = library.natives.flatMap(Natives::choose);
		if (classifier.isPresent()) {
			String notation = library.name + ':' + classifier.get().name;
			elements.add(makeDependencyElement(notation));
		}

		if (elements.isEmpty()) {
			throw new IllegalStateException("Library has nothing to download: " + library);
		}

		return elements.stream();
	}

	private static XmlBuilder.XmlElement makeDependencyElement(String notation) {
		String[] split = notation.split(":");
		String classifier = split.length > 3 ? split[3] : null;
		return makeDependencyElement(split[0], split[1], split[2], classifier);
	}

	private static XmlBuilder.XmlElement makeDependencyElement(String group, String artifact, String version, @Nullable String classifier) {
		List<XmlBuilder.XmlElement> children = new ArrayList<>(List.of(
				new XmlBuilder.XmlElement("groupId", group),
				new XmlBuilder.XmlElement("artifactId", artifact),
				new XmlBuilder.XmlElement("version", version),
				new XmlBuilder.XmlElement("scope", "compile")
		));

		if (classifier != null) {
			children.add(new XmlBuilder.XmlElement("classifier", classifier));
		}

		return new XmlBuilder.XmlElement("dependency", children);
	}
}
