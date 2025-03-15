package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.cache.storage.JarsStorage;
import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import io.github.cichlidmc.cichlid_gradle.util.XmlBuilder;
import io.github.cichlidmc.cichlid_gradle.util.XmlBuilder.XmlElement;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.rule.Features;
import io.github.cichlidmc.pistonmetaparser.rule.Rule;
import io.github.cichlidmc.pistonmetaparser.version.library.Artifact;
import io.github.cichlidmc.pistonmetaparser.version.library.Classifier;
import io.github.cichlidmc.pistonmetaparser.version.library.Library;
import io.github.cichlidmc.pistonmetaparser.version.library.Natives;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GenerateMetadataTask extends CacheTask {
	private final Distribution dist;
	private final JarsStorage storage;
	private final FullVersion version;

	protected GenerateMetadataTask(TaskContext context, Distribution dist, JarsStorage storage, FullVersion version) {
		super("Generate " + dist + " metadata", "Generate Ivy metadata for the " + dist, context);
		this.dist = dist;
		this.storage = storage;
		this.version = version;
	}

	@Override
	protected void doRun() throws IOException {
		String module = "minecraft-" + this.dist;
		Path output = this.storage.metadata(this.dist);

		XmlBuilder.create().add(
				new XmlElement("ivy-module", Map.of(
						"version", "2.0",
						"xmlns:m", "http://ant.apache.org/ivy/maven"
				), List.of(
						new XmlElement("info", Map.of(
								"organisation", CichlidCache.MINECRAFT_GROUP,
								"module", module,
								"revision", this.version.id
						)),
						new XmlElement("dependencies", this.makeDependencyElements())
				))
		).write(output);
	}

	private List<XmlElement> makeDependencyElements() {
		// bundler needs no dependencies
		if (this.dist == Distribution.BUNDLER)
			return List.of();

		List<XmlElement> elements = new ArrayList<>();

		for (Library library : this.version.libraries) {
			if (!Rule.test(library.rules, Features.EMPTY))
				continue;

			Optional<Artifact> artifact = library.artifact;
			if (artifact.isPresent()) {
				elements.add(makeDependencyElement(library.name, null));
			}

			Optional<Classifier> classifier = library.natives.flatMap(Natives::choose);
			classifier.ifPresent(value -> elements.add(
					makeDependencyElement(library.name, value.name)
			));
		}

		return elements;
	}

	private static XmlElement makeDependencyElement(String notation, @Nullable String classifier) {
		String[] split = notation.split(":");

		Map<String, String> attributes = new HashMap<>();
		attributes.put("org", split[0]);
		attributes.put("name", split[1]);
		attributes.put("rev", split[2]);

		List<XmlElement> children = classifier == null ? List.of() : List.of(new XmlElement("artifact", Map.of(
				"name", split[1],
				"type", "jar",
				"ext", "jar",
				"m:classifier", classifier
		)));

		return new XmlElement("dependency", attributes, children);
	}
}
