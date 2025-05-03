package fish.cichlidmc.cichlid_gradle.cache;

import fish.cichlidmc.cichlid_gradle.cache.storage.AssetStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.manifest.Version;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Interface for Cichlid's global Minecraft cache. Holds no state, just interfaces with the filesystem.
 * Layout is versioned, and each one can be found at {@code "root/v" + FORMAT}.
 * <p>
 * Layout:
 * <ul>
 *     <li>
 *         assets
 *         <ul>
 *             <li>indices</li>
 *             <li>objects</li>
 *             <li>.lock</li>
 *         </ul>
 *     </li>
 *     <li>
 *         $version
 *         <ul>
 *             <li>natives</li>
 *             <li>mappings</li>
 *             <li>jars</li>
 *             <li>runs</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public final class CichlidCache {
	public static final String MINECRAFT_GROUP = "net.minecraft";
	public static final String MINECRAFT_MODULE = "minecraft";

	public static final String LOCAL_CACHE_PROPERTY = "cichlid.use_local_cache";
	public static final int FORMAT = 1;
	public static final String PATH = "cichlid-gradle-cache/v" + FORMAT;

	public final Path root;
	// assets are shared by many versions, they're stored separately
	public final AssetStorage assets;

	private CichlidCache(Path root) {
		this.root = root;
		this.assets = new AssetStorage(this.root.resolve("assets"));
	}

	public VersionStorage getVersion(String version) {
		return new VersionStorage(this.root.resolve(version), version);
	}

	public void ensureVersionIsCached(String versionId) {
		// see if this version actually exists
		Version version = ManifestCache.getVersion(versionId);
		if (version == null)
			return;

		FullVersion fullVersion = ManifestCache.expand(version);

		TaskContext ctx = new TaskContext();
		this.assets.submitInitialTasks(fullVersion.assetIndex, ctx);

		VersionStorage storage = this.getVersion(versionId);
		storage.submitInitialTasks(fullVersion, ctx);

		ctx.report();

		// all tasks are done. If an exception wasn't thrown by report, everything was successful.
		storage.markComplete();
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
