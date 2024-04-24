package io.github.tropheusj.cichlid_gradle.minecraft.mcmaven;

import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.ExternalResourceAccessor;
import org.jetbrains.annotations.Nullable;

public class McMavenResourceAccessor implements ExternalResourceAccessor {
    @Nullable
    @Override
    public <T> T withContent(ExternalResourceName location, boolean revalidate, ExternalResource.ContentAndMetadataAction<T> action) throws ResourceException {
        System.out.println("withContent: " + location);
        return null;
    }

    @Nullable
    @Override
    public ExternalResourceMetaData getMetaData(ExternalResourceName location, boolean revalidate) throws ResourceException {
        System.out.println("getMetaData: " + location);
        return null;
    }
}
