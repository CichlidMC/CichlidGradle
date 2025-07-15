package fish.cichlidmc.cichlid_gradle.cache.storage;

import java.nio.file.Path;

public final class NativesStorage extends LockableStorage {
	public NativesStorage(Path root) {
		super(root);
	}
}
