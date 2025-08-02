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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Applies a function to every class within a jar file, and places the results in a new output jar.
 * Non-classes are copied as-is.
 */
public final class JarProcessor {
	// 🮲🮳
	public static void run(Path inputJar, ZipOutputStream output, ClassProcessor function) throws IOException {
		try (FileSystem inputFs = FileSystems.newFileSystem(inputJar)) {
			// jars should have 1 root
			Path inputRoot = FileUtils.getSingleRoot(inputFs);

			Input input = collectInput(inputRoot);
			for (ClassGroup group : input.groups.values()) {
				ClassGroup processed = function.apply(group);

				addEntry(output, processed.main());
				for (ClassEntry inner : processed.inner()) {
					addEntry(output, inner);
				}
			}

			for (Path nonClass : input.nonClasses) {
				String relative = FileUtils.safeRelativize(inputRoot, nonClass);
				output.putNextEntry(new ZipEntry(relative));
				FileUtils.copy(nonClass, output);
			}
		}
	}

	private static void addEntry(ZipOutputStream output, ClassEntry entry) throws IOException {
		output.putNextEntry(new ZipEntry(entry.fileName()));
		entry.content().write(output);
	}

	public static Input collectInput(Path inputRoot) throws IOException {
		Map<String, ClassGroup> groups = new HashMap<>();
		Collection<Path> nonClasses = new HashSet<>();

		FileUtils.walkFiles(inputRoot, entry -> {
			// net/minecraft/ClassName$Inner.(class/java)
			String name = FileUtils.safeRelativize(inputRoot, entry);

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
