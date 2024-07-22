package io.github.cichlidmc.cichlid_gradle.util;

import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ReadableContent;
import org.gradle.internal.resource.transfer.ExternalResourceUploader;

public class NoOpUploader implements ExternalResourceUploader {
    public static final NoOpUploader INSTANCE = new NoOpUploader();

    @Override
    public void upload(ReadableContent resource, ExternalResourceName destination) {
        throw new UnsupportedOperationException();
    }
}
