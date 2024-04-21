package io.github.tropheusj.cichlid_gradle.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class DirDeleter extends SimpleFileVisitor<Path> {
    public static void run(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walkFileTree(dir, new DirDeleter());
        }
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }
}
