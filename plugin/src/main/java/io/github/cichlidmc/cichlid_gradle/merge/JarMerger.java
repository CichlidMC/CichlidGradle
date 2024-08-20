package io.github.cichlidmc.cichlid_gradle.merge;

import io.github.cichlidmc.annotations.Dist;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.cichlid_gradle.util.IterableStream;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class JarMerger {
    public static void merge(Path clientPath, Path serverPath, Path mergedPath) throws IOException {
        FileUtils.ensureCreated(mergedPath);

        try (FileSystem client = FileSystems.newFileSystem(clientPath);
             FileSystem server = FileSystems.newFileSystem(serverPath);
             FileSystem merged = FileSystems.newFileSystem(mergedPath)) {

            Path clientRoot = client.getRootDirectories().iterator().next();
            Path serverRoot = server.getRootDirectories().iterator().next();
            Path mergedRoot = merged.getRootDirectories().iterator().next();

            // walk from both sides to get classes exclusive to both
            walk(clientRoot, serverRoot, mergedRoot, Dist.CLIENT);
            walk(serverRoot, clientRoot, mergedRoot, Dist.SERVER);
        }
    }

    private static void walk(Path dir, Path otherDir, Path mergedDir, Dist dist) throws IOException {
        try (IterableStream<Path> stream = new IterableStream<>(Files.list(dir))) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                Path otherPath = otherDir.resolve(fileName);
                Path mergedPath = mergedDir.resolve(fileName);

                if (Files.isDirectory(path)) {
                    walk(path, otherPath, mergedPath, dist);
                } else if (fileName.endsWith(".class")) {
                    ClassMerger.run(path, otherPath, mergedPath, dist);
                }
            }
        }
    }
}
