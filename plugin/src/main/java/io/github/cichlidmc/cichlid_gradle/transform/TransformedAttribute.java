package io.github.cichlidmc.cichlid_gradle.transform;

import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.DocsType;

// TODO: this whole system is currently dead code, waiting for source transforms to be figured out.
public class TransformedAttribute {
	public static final Attribute<Boolean> INSTANCE = Attribute.of("io.github.cichlidmc.cichlid_gradle.transformed", Boolean.class);

	public static void setup(Project project) {
		DependencyHandler deps = project.getDependencies();

		deps.getAttributesSchema().attribute(INSTANCE);

		deps.getArtifactTypes().named("jar").configure(
				def -> def.getAttributes().attribute(INSTANCE, false)
		);

		// deps.registerTransform(MinecraftTransformAction.class, spec -> {
		// 	Category library = project.getObjects().named(Category.class, Category.LIBRARY);
		//
		// 	spec.getFrom()
		// 			.attribute(INSTANCE, false)
		// 			.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar");
		// 	spec.getTo()
		// 			.attribute(INSTANCE, true)
		// 			.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar");
		// });

		deps.registerTransform(MinecraftSourcesTransformAction.class, spec -> {
			Category docs = project.getObjects().named(Category.class, Category.DOCUMENTATION);
			DocsType type = project.getObjects().named(DocsType.class, DocsType.SOURCES);

			spec.getFrom()
					.attribute(INSTANCE, false)
					.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
					.attribute(Category.CATEGORY_ATTRIBUTE, docs)
					.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, type);
			spec.getTo()
					.attribute(INSTANCE, true)
					.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
					.attribute(Category.CATEGORY_ATTRIBUTE, docs)
					.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, type);
		});
	}
}
