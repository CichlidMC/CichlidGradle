package io.github.cichlidmc.cichlid_gradle.util;

import java.io.IOException;

public interface IoSupplier<T> {
    T get() throws IOException;
}