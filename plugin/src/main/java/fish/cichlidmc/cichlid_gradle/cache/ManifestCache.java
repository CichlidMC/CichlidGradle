package fish.cichlidmc.cichlid_gradle.cache;

import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.PistonMeta;
import fish.cichlidmc.pistonmetaparser.VersionManifest;
import fish.cichlidmc.pistonmetaparser.manifest.Version;

/**
 * Wraps access to a cached manifest singleton in synchronization so it's safe to share
 */
public class ManifestCache {
	private static VersionManifest manifest;

	public static synchronized VersionManifest get() {
		if (manifest == null) {
			manifest = PistonMeta.fetch();
		}
		return manifest;
	}

	public static Version getVersion(String id) {
		VersionManifest manifest = get();
		synchronized (manifest) {
			return manifest.getVersion(id);
		}
	}

	public static FullVersion expand(Version version) {
		//noinspection SynchronizationOnLocalVariableOrMethodParameter - I'm pretty sure this is fine here
		synchronized (version) {
			return version.expand();
		}
	}
}
