package io.github.cichlidmc.cichlid_gradle.util;

import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.transfer.ExternalResourceLister;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NoOpResourceLister implements ExternalResourceLister {
    public static final NoOpResourceLister INSTANCE = new NoOpResourceLister();

    @Nullable
    @Override
    public List<String> list(ExternalResourceName parent) {
        throw new UnsupportedOperationException();
    }
}
