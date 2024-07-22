package io.github.cichlidmc.cichlid_gradle.minecraft.mcmaven;

import io.github.cichlidmc.cichlid_gradle.minecraft.MinecraftMaven;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ResourceExceptions;
import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.ExternalResourceAccessor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class McMavenResourceAccessor implements ExternalResourceAccessor {
    private final MinecraftMaven mcMaven;

    public McMavenResourceAccessor(File gradleHome) {
        Path path = gradleHome.toPath();
        this.mcMaven = MinecraftMaven.get(path);
    }

    @Nullable
    @Override
    public <T> T withContent(ExternalResourceName location, boolean revalidate,
                             ExternalResource.ContentAndMetadataAction<T> action) throws ResourceException {
        URI uri = location.getUri();
        Path file = this.mcMaven.getFile(uri);
        if (file == null)
            return null;

        try (InputStream stream = Files.newInputStream(file)) {
            return action.execute(stream, this.getMetaData(location, revalidate));
        } catch (IOException e) {
            throw ResourceExceptions.getFailed(uri, e);
        }
    }

    @Nullable
    @Override
    public ExternalResourceMetaData getMetaData(ExternalResourceName location, boolean revalidate) throws ResourceException {
        URI uri = location.getUri();
        Path file = this.mcMaven.getFile(uri);
        if (file == null)
            return null;

        try {
            return new DefaultExternalResourceMetaData(
                    uri,
                    new Date(Files.getLastModifiedTime(file).toMillis()),
                    Files.size(file),
                    null,
                    null,
                    HashCode.fromString(FileUtils.sha1(file)),
                    file.getFileName().toString(),
                    false
            );
        } catch (IOException e) {
            throw ResourceExceptions.getFailed(uri, e);
        }
    }
}
