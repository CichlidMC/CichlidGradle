package fish.cichlidmc.cichlid_gradle.merge;

import fish.cichlidmc.cichlid_gradle.util.IterableStream;
import fish.cichlidmc.distmarker.Dist;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class MergeSource {
    public final Dist dist;
    public final Path path;
    public final FileSystem fs;

    public MergeSource(Dist dist, Path path) throws IOException {
        this.dist = dist;
        this.path = path;
        this.fs = FileSystems.newFileSystem(path);
    }

    public void listEntries(Set<String> entries) throws IOException {
        Path root = this.fs.getRootDirectories().iterator().next();
        walk(root, "", entries);
    }

    public Path getEntry(String path) {
        return this.fs.getPath(path);
    }

    @Override
    public String toString() {
        return this.dist.toString().toLowerCase(Locale.ROOT);
    }

    private static void walk(Path path, String key, Set<String> entries) throws IOException {
        if (!Files.isDirectory(path)) {
            entries.add(key);
            return;
        }

        try (IterableStream<Path> stream = new IterableStream<>(Files.list(path))) {
            for (Path subpath : stream) {
                String dirName = subpath.getFileName().toString();
                if (dirName.equals("META-INF"))
                    continue;

                String subPathKey = key.isEmpty() ? dirName : key + '/' + dirName;
                walk(subpath, subPathKey, entries);
            }
        }
    }
}
