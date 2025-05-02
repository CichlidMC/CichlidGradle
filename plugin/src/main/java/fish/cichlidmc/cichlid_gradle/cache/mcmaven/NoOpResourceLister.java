package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.transfer.ExternalResourceLister;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum NoOpResourceLister implements ExternalResourceLister {
	INSTANCE;

	@Nullable
	@Override
	public List<String> list(ExternalResourceName parent) {
		throw new UnsupportedOperationException();
	}
}
