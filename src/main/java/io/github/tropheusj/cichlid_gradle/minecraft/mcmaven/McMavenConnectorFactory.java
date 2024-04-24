package io.github.tropheusj.cichlid_gradle.minecraft.mcmaven;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import io.github.tropheusj.cichlid_gradle.util.NoOpResourceLister;
import io.github.tropheusj.cichlid_gradle.util.NoOpUploader;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
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
		return new DefaultExternalResourceConnector(
				new McMavenResourceAccessor(),
				NoOpResourceLister.INSTANCE,
				NoOpUploader.INSTANCE
		);
	}

	public static void inject(RepositoryTransportFactory factory) {
		try {
			Field registeredProtocols = RepositoryTransportFactory.class.getDeclaredField("registeredProtocols");
			registeredProtocols.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<ResourceConnectorFactory> list = (List<ResourceConnectorFactory>) registeredProtocols.get(factory);
			list.add(new McMavenConnectorFactory());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Error accessing gradle internals, probably need an update", e);
		}
	}
}
