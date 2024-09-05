package io.github.cichlidmc.cichlid_gradle.merge;

import io.github.cichlidmc.annotations.Dist;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class JarMerger {
    private static final Logger logger = Logging.getLogger(JarMerger.class);

    public static void merge(List<MergeSource> sources, Path output) throws IOException {
        logger.quiet("Merging {} jars: {}", sources.size(), sources);
        FileUtils.ensureCreated(output);

        try (FileSystem merged = FileSystems.newFileSystem(output)) {
            Set<String> entries = new HashSet<>();
            for (MergeSource mergeSource : sources) {
                mergeSource.listEntries(entries);
            }

            for (String entry : entries) {
                Path dest = merged.getPath(entry);

                Map<MergeSource, Path> paths = new HashMap<>();
                for (MergeSource source : sources) {
                    Path path = source.getEntry(entry);
                    if (Files.exists(path)) {
                        paths.put(source, path);
                    }
                }

                if (paths.isEmpty()) {
                    throw new IllegalStateException("No paths exist for " + entry);
                } else if (paths.size() == 1) {
                    // exclusive
                    Map.Entry<MergeSource, Path> mapEntry = paths.entrySet().iterator().next();
                    Path source = mapEntry.getValue();
                    Dist dist = mapEntry.getKey().dist;
                    copyExclusiveEntry(source, dest, dist);
                } else {
                    mergeEntry(paths, dest);
                }
            }
        }
    }

    private static void copyExclusiveEntry(Path path, Path dest, Dist dist) throws IOException {
        if (path.toString().endsWith(".class")) {
            ClassMerger.copyExclusiveClass(path, dest, dist);
        } else {
            Files.copy(path, dest);
        }
    }

    private static void mergeEntry(Map<MergeSource, Path> sources, Path dest) throws IOException {
        if (allEqualContent(sources.values())) {
            // when file content is the same across all sources, just copy one over
            Path source = sources.values().iterator().next();
            Files.copy(source, dest);
        } else if (dest.toString().endsWith(".class")) {
            ClassMerger.mergeClass(sources, dest);
        } else {
            // uh oh
            throw new IllegalStateException("Entry has conflicting content between sources: " + dest);
        }
    }

    private static boolean allEqualContent(Collection<Path> files) {
        return allEqual(files, FileUtils::readAllBytesUnchecked, Arrays::equals);
    }

    public static <T, V> boolean allEqual(Collection<T> collection, Function<T, V> extractor, BiPredicate<V, V> equalTest) {
        V first = extractor.apply(collection.iterator().next());
        for (T t : collection) {
            if (!equalTest.test(first, extractor.apply(t))) {
                return false;
            }
        }
        return true;
    }
}
