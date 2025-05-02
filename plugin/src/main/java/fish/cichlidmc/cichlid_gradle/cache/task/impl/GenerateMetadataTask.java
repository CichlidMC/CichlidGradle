package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.JarsStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.XmlBuilder;
import fish.cichlidmc.cichlid_gradle.util.XmlBuilder.XmlElement;
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

public class GenerateMetadataTask extends CacheTask {
	private final Distribution dist;
	private final JarsStorage storage;
	private final FullVersion version;

	protected GenerateMetadataTask(TaskContext context, Distribution dist, JarsStorage storage, FullVersion version) {
		super("Generate " + dist + " metadata", "Generate Maven metadata for the " + dist, context);
		this.dist = dist;
		this.storage = storage;
		this.version = version;
	}

	@Override
	protected void doRun() throws IOException {
		Path output = this.storage.metadata(this.dist);

		XmlBuilder.create().add(new XmlElement("project", List.of(
				new XmlElement("modelVersion", "4.0.0"),
				new XmlElement("groupId", "net.minecraft"),
				new XmlElement("artifactId", "minecraft-" + this.dist),
				new XmlElement("version", this.version.id),
				new XmlElement("dependencies", this.version.libraries.stream().flatMap(this::makeDependencyElements).toList())
		))).write(output);
	}

	private Stream<XmlElement> makeDependencyElements(Library library) {
		// bundler does not have dependencies
		if (this.dist == Distribution.BUNDLER)
			return Stream.empty();

		// check rules first
		if (!Rule.test(library.rules, Features.EMPTY))
			return Stream.empty();

		List<XmlElement> elements = new ArrayList<>();
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
