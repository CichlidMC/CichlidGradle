package io.github.cichlidmc.cichlid_gradle.cache;

import java.nio.file.Path;

import org.gradle.api.invocation.Gradle;

public class CichlidCache {
	public static final String PATH = "caches/cichlid-gradle";

	public final AssetStorage assets;
	public final NativesStorage natives;
	public final RunsStorage runs;

	private CichlidCache(Path root) {
		this.assets = AssetStorage.get(root);
		this.natives = NativesStorage.get(root);
		this.runs = RunsStorage.get(root);
	}

	public static CichlidCache get(Gradle gradle) {
		Path gradleDir = gradle.getGradleUserHomeDir().toPath();
		return new CichlidCache(gradleDir.resolve(PATH));
	}
}
