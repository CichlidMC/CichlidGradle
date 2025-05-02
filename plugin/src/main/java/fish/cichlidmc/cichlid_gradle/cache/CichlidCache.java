package fish.cichlidmc.cichlid_gradle.cache;

import fish.cichlidmc.cichlid_gradle.cache.storage.AssetStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.manifest.Version;
import org.gradle.api.Project;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

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
	// pattern is relative to root
	public static final String MINECRAFT_GROUP = "net.minecraft";
	public static final Set<String> MINECRAFT_MODULES = Set.of(
			"minecraft-client", "minecraft-server", "minecraft-merged", "minecraft-bundler"
	);

	public static final String PROJECT_CACHE_PROPERTY = "cichlid.use_project_cache";
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
		if (Objects.equals(project.findProperty(PROJECT_CACHE_PROPERTY), "true")) {
			return project.file(".gradle").toPath();
		} else {
			return project.getGradle().getGradleUserHomeDir().toPath().resolve("caches");
		}
	}
}
