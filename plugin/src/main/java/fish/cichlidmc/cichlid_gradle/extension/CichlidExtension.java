package fish.cichlidmc.cichlid_gradle.extension;

import org.gradle.api.Project;

public interface CichlidExtension {
	static void setup(Project project) {
		project.getExtensions().create(CichlidExtension.class, "cichlid", CichlidExtensionImpl.class);
	}

	static CichlidExtension get(Project project) {
		return project.getExtensions().getByType(CichlidExtension.class);
	}
}
