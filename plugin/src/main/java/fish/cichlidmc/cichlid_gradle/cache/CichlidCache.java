package fish.cichlidmc.cichlid_gradle.cache;

import fish.cichlidmc.cichlid_gradle.cache.storage.AssetStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.DecompiledClassStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.PomTemplateStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.ReassembledJarStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.TransformedClassStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Interface for Cichlid's global Minecraft cache. Holds no state, just interfaces with the filesystem.
 * Layout is versioned, and each one can be found at {@code "root/v" + FORMAT}.
 */
public final class CichlidCache {
	public static final String LOCAL_CACHE_PROPERTY = "cichlid.use_local_cache";
	public static final int FORMAT = 1;
	public static final String PATH = "cichlid-gradle-cache/v" + FORMAT;

	private static final Logger logger = Logging.getLogger(CichlidCache.class);

	public final Path root;
	public final AssetStorage assets;
	public final ReassembledJarStorage reassembledJars;
	public final DecompiledClassStorage decompiledClasses;
	public final TransformedClassStorage transformedClasses;
	public final PomTemplateStorage pomTemplates;

	private CichlidCache(Path root) {
		this.root = root;
		this.assets = new AssetStorage(root.resolve("assets"));
		this.reassembledJars = new ReassembledJarStorage(root.resolve("reassembled"));
		this.decompiledClasses = new DecompiledClassStorage(root.resolve("decompiled"));
		this.transformedClasses = new TransformedClassStorage(root.resolve("transformed"));
		this.pomTemplates = new PomTemplateStorage(root.resolve("poms"));
	}

	public VersionStorage getVersion(String version) {
		return new VersionStorage(this.root.resolve("versions").resolve(version), version);
	}

	public static CichlidCache get(Project project) {
		return get(getPath(project));
	}

	public static CichlidCache get(Path path) {
		return new CichlidCache(path);
	}

	public static Path getPath(Project project) {
		Path cache = getGradleCache(project);
		return cache.resolve(PATH);
	}

	private static Path getGradleCache(Project project) {
		Gradle gradle = project.getGradle();
		if (Objects.equals(project.findProperty(LOCAL_CACHE_PROPERTY), "true")) {
			return gradle.getRootProject().file(".gradle").toPath();
		} else {
			return gradle.getGradleUserHomeDir().toPath().resolve("caches");
		}
	}
}
