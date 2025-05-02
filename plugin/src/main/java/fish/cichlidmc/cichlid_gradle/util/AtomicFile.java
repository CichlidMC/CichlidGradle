package fish.cichlidmc.cichlid_gradle.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record AtomicFile(Path temp, Path target) implements AutoCloseable {
    public AtomicFile(Path target) throws IOException {
        this(FileUtils.createTempFile(target), target);
    }

    public void commit() throws IOException {
        FileUtils.move(this.temp, this.target);
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(this.temp);
    }
}
