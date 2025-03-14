package io.github.cichlidmc.cichlid_gradle.cache;

import java.nio.file.Path;
import java.util.Objects;

import org.gradle.api.Project;

public class CichlidCache {
	public static final String PROJECT_CACHE_PROPERTY = "cichlid.use_project_cache";
	public static final String PATH = "cichlid-gradle";

	public final AssetStorage assets;
	public final NativesStorage natives;
	public final RunsStorage runs;
	public final MinecraftMaven maven;

	private CichlidCache(Path root) {
		Path mc = root.resolve("minecraft");
		this.assets = new AssetStorage(mc.resolve("assets"));
		this.natives = new NativesStorage(mc.resolve("natives"));
		this.runs = new RunsStorage(mc.resolve("runs"));
		this.maven = new MinecraftMaven(mc.resolve(MinecraftMaven.PATH), this);
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
