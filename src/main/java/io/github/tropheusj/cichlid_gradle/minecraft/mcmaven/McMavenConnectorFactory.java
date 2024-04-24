package io.github.tropheusj.cichlid_gradle.minecraft.mcmaven;

import java.util.Set;

import org.gradle.authentication.Authentication;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.connector.ResourceConnectorSpecification;
import org.gradle.internal.resource.transfer.DefaultExternalResourceConnector;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;

public class McMavenConnectorFactory implements ResourceConnectorFactory {
	@Override
	public Set<String> getSupportedProtocols() {
		return Set.of("mcmaven");
	}

	@Override
	public Set<Class<? extends Authentication>> getSupportedAuthentication() {
		return Set.of();
	}

	@Override
	public ExternalResourceConnector createResourceConnector(ResourceConnectorSpecification connectionDetails) {
		return null;//new DefaultExternalResourceConnector(accessor, lister, uploader);
	}
}
