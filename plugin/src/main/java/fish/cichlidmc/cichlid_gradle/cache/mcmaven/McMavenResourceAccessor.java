package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ResourceExceptions;
import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.ExternalResourceAccessor;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;

public final class McMavenResourceAccessor implements ExternalResourceAccessor {
	private final MinecraftMaven mcMaven;

	public McMavenResourceAccessor(MinecraftMaven mcMaven) {
		this.mcMaven = mcMaven;
	}

	@Nullable
	@Override
	public <T> T withContent(ExternalResourceName location, boolean revalidate,
							 ExternalResource.ContentAndMetadataAction<T> action) throws ResourceException {
		URI uri = location.getUri();
		System.out.println("checking " + uri);

		try {
			InputStream stream = this.mcMaven.get(uri);
			if (stream == null)
				return null;

			return action.execute(stream, this.getMetaData(location, revalidate));
		} catch (Exception e) {
			throw ResourceExceptions.getFailed(uri, e);
		}
	}

	@Nullable
	@Override
	public ExternalResourceMetaData getMetaData(ExternalResourceName location, boolean revalidate) throws ResourceException {
		URI uri = location.getUri();
		System.out.println("checking " + uri);

		try {
			return this.mcMaven.get(uri) == null ? null : new DefaultExternalResourceMetaData(uri, -1, -1);
		} catch (Exception e) {
			throw ResourceExceptions.getFailed(uri, e);
		}
	}
}
