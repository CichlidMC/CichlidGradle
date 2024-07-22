package io.github.cichlidmc.cichlid_gradle.pistonmeta.test;

import io.github.cichlidmc.cichlid_gradle.pistonmeta.VersionManifest;

public class Tester {
	public static void main(String[] args) {
		VersionManifest manifest = VersionManifest.fetch();
		for (VersionManifest.Version version : manifest.versions()) {
			try {
				version.expand();
			} catch (Throwable t) {
				System.out.println("failed to expand " + version.id());
				throw t;
			}
		}
	}
}
