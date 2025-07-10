package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.XmlBuilder;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.rule.Features;
import fish.cichlidmc.pistonmetaparser.rule.Rule;
import fish.cichlidmc.pistonmetaparser.version.library.Artifact;
import fish.cichlidmc.pistonmetaparser.version.library.Classifier;
import fish.cichlidmc.pistonmetaparser.version.library.Library;
import fish.cichlidmc.pistonmetaparser.version.library.Natives;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class PomGenerator {
	public static final String VERSION_PLACEHOLDER = "${version}";

	public static void generate(FullVersion version, Distribution dist, Path output) throws IOException {
		XmlBuilder.create().add(new XmlBuilder.XmlElement("project", List.of(
				new XmlBuilder.XmlElement("modelVersion", "4.0.0"),
				new XmlBuilder.XmlElement("groupId", "net.minecraft"),
				new XmlBuilder.XmlElement("artifactId", "minecraft-" + dist.name),
				new XmlBuilder.XmlElement("version", VERSION_PLACEHOLDER),
				new XmlBuilder.XmlElement("dependencies", version.libraries.stream().flatMap(PomGenerator::makeDependencyElements).toList())
		))).write(output);
	}

	private static Stream<XmlBuilder.XmlElement> makeDependencyElements(Library library) {
		// check rules first
		if (!Rule.test(library.rules, Features.EMPTY))
			return Stream.empty();

		List<XmlBuilder.XmlElement> elements = new ArrayList<>();
		Optional<Artifact> artifact = library.artifact;
		if (artifact.isPresent()) {
			elements.add(makeDependencyXml(library.name));
		}

		Optional<Classifier> classifier = library.natives.flatMap(Natives::choose);
		if (classifier.isPresent()) {
			String notation = library.name + ':' + classifier.get().name;
			elements.add(makeDependencyXml(notation));
		}

		if (elements.isEmpty()) {
			throw new IllegalStateException("Library has nothing to download: " + library);
		}

		return elements.stream();
	}

	private static XmlBuilder.XmlElement makeDependencyXml(String notation) {
		String[] split = notation.split(":");
		XmlBuilder.XmlElement element = new XmlBuilder.XmlElement("dependency", new ArrayList<>(List.of(
				new XmlBuilder.XmlElement("groupId", split[0]),
				new XmlBuilder.XmlElement("artifactId", split[1]),
				new XmlBuilder.XmlElement("version", split[2]),
				new XmlBuilder.XmlElement("scope", "compile")
		)));
		if (split.length > 3) {
			element.children().add(new XmlBuilder.XmlElement("classifier", split[3]));
		}
		return element;
	}
}
