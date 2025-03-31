package io.github.cichlidmc.cichlid_gradle.transform;

import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;

public class TransformedAttribute {
	public static final Attribute<Boolean> INSTANCE = Attribute.of("io.github.cichlidmc.cichlid_gradle.transformed", Boolean.class);

	public static void setup(Project project) {
		DependencyHandler deps = project.getDependencies();

		deps.getAttributesSchema().attribute(INSTANCE);

		deps.getArtifactTypes().named("jar").configure(
				def -> def.getAttributes().attribute(INSTANCE, false)
		);

		deps.registerTransform(SushiTransformAction.class, spec -> {
			spec.getFrom()
					.attribute(INSTANCE, false)
					.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar");
			spec.getTo()
					.attribute(INSTANCE, true)
					.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar");
		});
	}
}
