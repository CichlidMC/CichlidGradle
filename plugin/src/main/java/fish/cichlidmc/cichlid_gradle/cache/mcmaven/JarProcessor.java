package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import fish.cichlidmc.cichlid_gradle.util.Pair;
import fish.cichlidmc.cichlid_gradle.util.Utils;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Applies a function to every class within a jar file, and places the results in a new output jar.
 * Non-classes are copied as-is.
 */
public final class JarProcessor {
	public static void run(Path inputJar, Path outputJar, CacheFunction function) throws IOException {
		FileUtils.initEmptyZip(outputJar);

		try (FileSystem inputFs = FileSystems.newFileSystem(inputJar); FileSystem outputFs = FileSystems.newFileSystem(outputJar)) {
			// jars should have 1 root
			Path inputRoot = Utils.getOnly(inputFs.getRootDirectories());
			Path outputRoot = Utils.getOnly(outputFs.getRootDirectories());

			Input input = collectInput(inputRoot);
			for (ClassGroup group : input.groups.values()) {
				ClassGroup processed = function.apply(group);

				addEntry(outputRoot, processed.main);
				for (ClassEntry inner : processed.inner) {
					addEntry(outputRoot, inner);
				}
			}

			for (Path nonClass : input.nonClasses) {
				Path relative = inputRoot.relativize(nonClass);
				Path target = outputRoot.resolve(relative);
				FileUtils.copy(nonClass, target);
			}
		}
	}

	private static void addEntry(Path outputRoot, ClassEntry entry) throws IOException {
		Path target = outputRoot.resolve(entry.fileName);
		FileUtils.copy(entry.content, target);
	}

	public static Input collectInput(Path inputRoot) throws IOException {
		Map<String, ClassGroup> groups = new HashMap<>();
		Collection<Path> nonClasses = new HashSet<>();

		Files.walkFileTree(inputRoot, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path entry, BasicFileAttributes attrs) throws IOException {
				// net/minecraft/ClassName$Inner.(class/java)
				String name = inputRoot.relativize(entry).toString();

				if (!name.endsWith(".java") && !name.endsWith(".class")) {
					nonClasses.add(entry);
					return FileVisitResult.CONTINUE;
				}

				Pair<String, String> split = Utils.splitLast(name, '.');
				Objects.requireNonNull(split);

				// net/minecraft/ClassName$Inner
				String fqn = split.left();
				// .(class/java)
				String extension = split.right();
				// net/minecraft/ClassName
				String outerName = Utils.until(fqn, '$');

				Path outerPath = inputRoot.resolve(outerName + extension);
				FileUtils.assertExists(outerPath);

				groups.compute(outerName, ($, group) -> {
					if (group == null) {
						ClassEntry main = new ClassEntry(outerName + extension, outerPath);
						group = new ClassGroup(main, new ArrayList<>());
					}

					if (!fqn.equals(outerName)) {
						// this is an inner class
						group.inner.add(new ClassEntry(name, entry));
					}

					return group;
				});

				return FileVisitResult.CONTINUE;
			}
		});

		return new Input(groups, nonClasses);
	}

	public record Input(Map<String, ClassGroup> groups, Collection<Path> nonClasses) {
	}

	/**
	 * A class contained in the input jar, grouped with all of its inner classes.
	 */
	public record ClassGroup(ClassEntry main, Collection<ClassEntry> inner) {
		public ClassGroup(ClassEntry main) {
			this(main, List.of());
		}

		public String hash() throws IOException {
			List<Path> paths = this.inner.stream()
					.sorted(Comparator.comparing(ClassEntry::fileName))
					.map(ClassEntry::content)
					// collect into a mutable list
					.collect(Collectors.toCollection(ArrayList::new));
			paths.addFirst(this.main.content);

			return Encoding.BASE_FUNNY.encode(HashAlgorithm.SHA256.hash(paths));
		}
	}

	/**
	 * @param fileName the fully qualified class name + extension, ex. {@code net/minecraft/ClassName$Inner.class}
	 * @param content path to the file to use as the source of this class's content
	 */
	public record ClassEntry(String fileName, Path content) {
	}

	@FunctionalInterface
	public interface CacheFunction {
		ClassGroup apply(ClassGroup group) throws IOException;
	}
}
