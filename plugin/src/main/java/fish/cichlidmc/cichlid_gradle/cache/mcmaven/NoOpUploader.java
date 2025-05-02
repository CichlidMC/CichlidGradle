package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ReadableContent;
import org.gradle.internal.resource.transfer.ExternalResourceUploader;

public enum NoOpUploader implements ExternalResourceUploader {
	INSTANCE;

	@Override
	public void upload(ReadableContent resource, ExternalResourceName destination) {
		throw new UnsupportedOperationException();
	}
}
