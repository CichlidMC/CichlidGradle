package fish.cichlidmc.cichlid_gradle.merge;

import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.distmarker.Dist;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JarMerger {
    public static void merge(List<MergeSource> sources, ZipOutputStream output) throws IOException {
        Set<String> entries = new HashSet<>();
        for (MergeSource mergeSource : sources) {
            mergeSource.listEntries(entries);
        }

        for (String entry : entries) {
            output.putNextEntry(new ZipEntry(entry));

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
                copyExclusiveEntry(source, output, dist);
            } else {
                // present in >1 source
                // check for case where not exclusive, but not present everywhere
                // this is not representable currently
                int missing = sources.size() - paths.size();
                if (missing >= 2) {
                    throw new IllegalStateException("Entry is both non-exclusive and missing from some sources: " + entry);
                }
                mergeEntry(paths, entry, output);
            }
        }
    }

    private static void copyExclusiveEntry(Path path, OutputStream output, Dist dist) throws IOException {
        if (path.toString().endsWith(".class")) {
            ClassMerger.copyExclusiveClass(path, output, dist);
        } else {
            FileUtils.copy(path, output);
        }
    }

    private static void mergeEntry(Map<MergeSource, Path> sources, String entry, OutputStream output) throws IOException {
        if (allEqualContent(sources.values())) {
            // when file content is the same across all sources, just copy one over
            Path source = sources.values().iterator().next();
            FileUtils.copy(source, output);
        } else if (entry.endsWith(".class")) {
            ClassMerger.mergeClass(sources, output);
        } else {
            // uh oh
            throw new IllegalStateException("Entry has conflicting content between sources: " + entry);
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
