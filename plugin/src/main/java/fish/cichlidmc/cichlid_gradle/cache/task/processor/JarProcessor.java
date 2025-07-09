package fish.cichlidmc.cichlid_gradle.cache.task.processor;

import fish.cichlidmc.cichlid_gradle.util.Pair;
import fish.cichlidmc.cichlid_gradle.util.Utils;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Applies a function to every class within a jar file, and places the results in a new output jar.
 * Non-classes are copied as-is.
 */
public final class JarProcessor {
	// 🮲🮳
	public static void run(Path inputJar, Path outputJar, ClassProcessor function) throws IOException {
		FileUtils.initEmptyZip(outputJar);

		try (FileSystem inputFs = FileSystems.newFileSystem(inputJar); FileSystem outputFs = FileSystems.newFileSystem(outputJar)) {
			// jars should have 1 root
			Path inputRoot = FileUtils.getSingleRoot(inputFs);
			Path outputRoot = FileUtils.getSingleRoot(outputFs);

			Input input = collectInput(inputRoot);
			for (ClassGroup group : input.groups.values()) {
				ClassGroup processed = function.apply(group);

				addEntry(outputRoot, processed.main());
				for (ClassEntry inner : processed.inner()) {
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
		Path target = outputRoot.resolve(entry.fileName());
		entry.content().write(target);
	}

	public static Input collectInput(Path inputRoot) throws IOException {
		Map<String, ClassGroup> groups = new HashMap<>();
		Collection<Path> nonClasses = new HashSet<>();

		FileUtils.walkFiles(inputRoot, entry -> {
			// net/minecraft/ClassName$Inner.(class/java)
			String name = inputRoot.relativize(entry).toString();

			if (!name.endsWith(".java") && !name.endsWith(".class")) {
				nonClasses.add(entry);
				return;
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
					group.inner().add(new ClassEntry(name, entry));
				}

				return group;
			});
		});

		return new Input(groups, nonClasses);
	}

	public record Input(Map<String, ClassGroup> groups, Collection<Path> nonClasses) {
	}

	@FunctionalInterface
	public interface ClassProcessor {
		ClassGroup apply(ClassGroup group) throws IOException;
	}
}
