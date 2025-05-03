package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinition;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.authentication.Authentication;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.connector.ResourceConnectorSpecification;
import org.gradle.internal.resource.transfer.DefaultExternalResourceConnector;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class McMavenConnectorFactory implements ResourceConnectorFactory {
	public final String protocol;
	private final ExternalResourceConnector connector;

	public McMavenConnectorFactory(String protocol, MinecraftMaven mcMaven) {
		this.protocol = protocol;
		this.connector = new DefaultExternalResourceConnector(
				new McMavenResourceAccessor(mcMaven),
				NoOpResourceLister.INSTANCE,
				NoOpUploader.INSTANCE
		);
	}

	@Override
	public Set<String> getSupportedProtocols() {
		return Set.of(this.protocol);
	}

	@Override
	public Set<Class<? extends Authentication>> getSupportedAuthentication() {
		return Set.of();
	}

	@Override
	public ExternalResourceConnector createResourceConnector(ResourceConnectorSpecification connectionDetails) {
		return this.connector;
	}

	// built-in implementations use ServiceLoader, but that only works for Gradle modules.
	// a new one is registered for each project anyway. Each has a unique protocol based on project path.
	public static void inject(RepositoryTransportFactory factory, Project project) {
		List<ResourceConnectorFactory> list = extractFactories(factory);
		Set<String> protocols = collectProtocols(list);

		String protocol = MinecraftMaven.createProtocol(project);
		if (protocols.contains(protocol)) {
			throw new InvalidUserDataException("Multiple projects generate the URI protocol '" + protocol + "', including " + project.getPath());
		}

		CichlidCache cache = CichlidCache.get(project);
		NamedDomainObjectContainer<MinecraftDefinition> defs = MinecraftDefinition.getExtension(project);
		MinecraftMaven mcMaven = new MinecraftMaven(defs, cache);

		list.add(new McMavenConnectorFactory(protocol, mcMaven));
	}

	@SuppressWarnings("unchecked")
	private static List<ResourceConnectorFactory> extractFactories(RepositoryTransportFactory factory) {
		try {
			Field registeredProtocols = RepositoryTransportFactory.class.getDeclaredField("registeredProtocols");
			registeredProtocols.setAccessible(true);
			return (List<ResourceConnectorFactory>) registeredProtocols.get(factory);
		} catch (ReflectiveOperationException | ClassCastException e) {
			throw new RuntimeException("Error accessing Gradle internals! You probably need to update Gradle, CichlidGradle, or both", e);
		}
	}

	private static Set<String> collectProtocols(List<ResourceConnectorFactory> factories) {
		Set<String> protocols = new HashSet<>();
		for (ResourceConnectorFactory factory : factories) {
			if (factory instanceof McMavenConnectorFactory mcMavenFactory) {
				protocols.add(mcMavenFactory.protocol);
			}
		}
		return protocols;
	}
}
