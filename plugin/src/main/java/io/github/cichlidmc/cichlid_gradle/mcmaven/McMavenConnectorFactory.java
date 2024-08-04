package io.github.cichlidmc.cichlid_gradle.mcmaven;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.github.cichlidmc.cichlid_gradle.util.NoOpResourceLister;
import io.github.cichlidmc.cichlid_gradle.util.NoOpUploader;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.invocation.Gradle;
import org.gradle.authentication.Authentication;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.connector.ResourceConnectorSpecification;
import org.gradle.internal.resource.transfer.DefaultExternalResourceConnector;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;

public class McMavenConnectorFactory implements ResourceConnectorFactory {
	private final Gradle gradle;

    public McMavenConnectorFactory(Gradle gradle) {
        this.gradle = gradle;
    }

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
				new McMavenResourceAccessor(this.gradle),
				NoOpResourceLister.INSTANCE,
				NoOpUploader.INSTANCE
		);
	}

	public static void inject(Project project) {
		Smuggler smuggler = project.getObjects().newInstance(Smuggler.class);

		try {
			Field registeredProtocols = RepositoryTransportFactory.class.getDeclaredField("registeredProtocols");
			registeredProtocols.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<ResourceConnectorFactory> list = (List<ResourceConnectorFactory>) registeredProtocols.get(smuggler.factory);
			// don't add if it already exists
			if (list.stream().anyMatch(f -> f instanceof McMavenConnectorFactory))
				return;

			list.add(new McMavenConnectorFactory(project.getGradle()));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Error accessing gradle internals, probably need an update", e);
		}
	}


	@SuppressWarnings("ClassCanBeRecord")
	public static class Smuggler {
		public final RepositoryTransportFactory factory;

		@Inject
		public Smuggler(RepositoryTransportFactory factory) {
			this.factory = factory;
		}
	}
}
