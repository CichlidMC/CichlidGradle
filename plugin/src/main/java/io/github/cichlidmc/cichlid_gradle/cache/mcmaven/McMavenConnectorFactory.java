package io.github.cichlidmc.cichlid_gradle.cache.mcmaven;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.authentication.Authentication;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.connector.ResourceConnectorSpecification;
import org.gradle.internal.resource.transfer.DefaultExternalResourceConnector;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class McMavenConnectorFactory implements ResourceConnectorFactory {
	public static final Set<String> PROTOCOLS = Set.of(MinecraftMaven.PROTOCOL);

	private final CichlidCache cache;

	public McMavenConnectorFactory(CichlidCache cache) {
		this.cache = cache;
	}

	@Override
	public Set<String> getSupportedProtocols() {
		return PROTOCOLS;
	}

	@Override
	public Set<Class<? extends Authentication>> getSupportedAuthentication() {
		return Set.of();
	}

	@Override
	public ExternalResourceConnector createResourceConnector(ResourceConnectorSpecification connectionDetails) {
		return new DefaultExternalResourceConnector(
				new McMavenResourceAccessor(this.cache),
				NoOpResourceLister.INSTANCE,
				NoOpUploader.INSTANCE
		);
	}

	// built-in implementations use ServiceLoader, but that only works for Gradle modules.
	public static void inject(RepositoryTransportFactory factory, CichlidCache cache) {
		try {
			Field registeredProtocols = RepositoryTransportFactory.class.getDeclaredField("registeredProtocols");
			registeredProtocols.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<ResourceConnectorFactory> list = (List<ResourceConnectorFactory>) registeredProtocols.get(factory);
			// don't add if it already exists
			if (list.stream().anyMatch(f -> f instanceof McMavenConnectorFactory))
				return;

			list.add(new McMavenConnectorFactory(cache));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Error accessing Gradle internals! You probably need to update Gradle, CichlidGradle, or both", e);
		}
	}
}
