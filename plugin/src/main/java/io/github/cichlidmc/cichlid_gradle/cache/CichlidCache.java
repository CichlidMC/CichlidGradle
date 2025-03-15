package io.github.cichlidmc.cichlid_gradle.cache;

import io.github.cichlidmc.cichlid_gradle.cache.storage.AssetStorage;
import io.github.cichlidmc.cichlid_gradle.cache.storage.VersionStorage;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.manifest.Version;
import org.gradle.api.Project;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Interface for Cichlid's global Minecraft cache. Holds no state, just interfaces with the filesystem.
 * Layout:
 * - root
 *   - v1
 *     - assets
 *       - indices
 *       - objects
 *       - .lock
 *     - <version>
 *       - natives
 *       - mappings
 *       - jars
 *       - runs
 */
public class CichlidCache {
	// pattern is relative to root
	public static final String IVY_PATTERN = "[revision]/jars/[module]-[revision].[ext]";
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
