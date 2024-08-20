package io.github.cichlidmc.cichlid_gradle.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.stream.Stream;

public class IterableStream<T> implements Iterable<T>, AutoCloseable {
    private final Stream<T> stream;

    public IterableStream(Stream<T> stream) {
        this.stream = stream;
    }

    @Override
    public void close() {
        this.stream.close();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.stream.iterator();
    }
}
