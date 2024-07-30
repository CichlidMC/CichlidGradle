package io.github.cichlidmc.cichlid_gradle.pistonmeta.test;

import java.util.HashSet;
import java.util.Set;

import io.github.cichlidmc.cichlid_gradle.pistonmeta.FullVersion;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.VersionManifest;

public class Tester {
	public static void main(String[] args) {
		VersionManifest manifest = VersionManifest.fetch();
		Set<String> assetIndices = new HashSet<>();
		for (VersionManifest.Version version : manifest.versions()) {
			try {
				FullVersion fullVersion = version.expand();
				if (!assetIndices.contains(fullVersion.assets())) {
					assetIndices.add(fullVersion.assets());
					fullVersion.assetIndex().expand();
				}
			} catch (Throwable t) {
				System.out.println("failed to expand " + version.id());
				throw t;
			}
		}
	}
}
