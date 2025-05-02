package fish.cichlidmc.cichlid_gradle.merge.element;

import fish.cichlidmc.cichlid_gradle.merge.JarMerger;
import fish.cichlidmc.cichlid_gradle.merge.MergeSource;
import fish.cichlidmc.distmarker.Dist;
import fish.cichlidmc.distmarker.Distribution;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class InterfacesMerger {
	public static void merge(Map<MergeSource, ClassNode> classes, ClassNode output) {
		// no need to do anything when they already match
		if (JarMerger.allEqual(classes.values(), node -> node.interfaces, Objects::equals))
			return;

		if (true) {
			// I can't find anything wrong with this code, but the added annotations
			// never show up. Might be an ASM issue? From a GitHub search, I couldn't
			// find a single use of class type annotations, so it might just be broken.
			// FIXME: make a minimal reproduction case and submit an issue
			throw new RuntimeException("Interface merging is currently broken.");
		}

		System.out.println("merging interfaces on " + output.name);
		output.interfaces.clear();

		// handling this is annoying and I don't think they're ever present.
		// throw in case that assumption is wrong.
		assertEmpty(output.visibleTypeAnnotations, "visibleTypeAnnotations", output.name);
		assertEmpty(output.invisibleTypeAnnotations, "invisibleTypeAnnotations", output.name);

		List<InterfaceInfo> interfaces = getAllInterfaces(classes);
		System.out.println(interfaces);
		for (int i = 0; i < interfaces.size(); i++) {
			InterfaceInfo info = interfaces.get(i);
			output.interfaces.add(info.name);
			if (info.dist == null)
				continue;

			if (output.visibleTypeAnnotations == null) {
				output.visibleTypeAnnotations = new ArrayList<>();
			}

			TypeReference reference = TypeReference.newSuperTypeReference(i);
			output.visibleTypeAnnotations.add(makeDistAnnotation(reference, info.dist));
		}
	}

	private static List<InterfaceInfo> getAllInterfaces(Map<MergeSource, ClassNode> classes) {
		// start with all common interfaces
		List<String> common = getCommonInterfaces(classes);
		List<InterfaceInfo> infos = common.stream()
				.map(InterfaceInfo::new)
				.collect(Collectors.toCollection(ArrayList::new));

		// then add all exclusives, one dist at a time
		classes.forEach((source, node) -> {
			for (String classInterface : node.interfaces) {
				if (!common.contains(classInterface)) {
					infos.add(new InterfaceInfo(classInterface, source.dist));
				}
			}
		});

		return infos;
	}

	private static List<String> getCommonInterfaces(Map<MergeSource, ClassNode> classes) {
		// pick an arbitrary base to initialize the common set
		MergeSource anySource = classes.keySet().iterator().next();
		ClassNode base = classes.get(anySource);

		List<String> common = new ArrayList<>(base.interfaces);
		// go through each source and remove interfaces that aren't actually common
		for (ClassNode source : classes.values()) {
			// skip the one that was used as a base
			if (source != base) {
				common.removeIf(commonInterface -> !source.interfaces.contains(commonInterface));
			}
		}

		return common;
	}

	private static TypeAnnotationNode makeDistAnnotation(TypeReference reference, Dist dist) {
		TypeAnnotationNode node = new TypeAnnotationNode(
				reference.getSuperTypeIndex(), null, Type.getDescriptor(Distribution.class)
		);

		node.visitEnum("value", Type.getDescriptor(Dist.class), dist.name());
		return node;
	}

	private static void assertEmpty(@Nullable Collection<?> collection, String name, String className) {
		if (collection != null && !collection.isEmpty()) {
			throw new IllegalStateException(name + " is not empty in " + className);
		}
	}

	private record InterfaceInfo(String name, @Nullable Dist dist) {
		private InterfaceInfo(String name) {
			this(name, null);
		}
	}
}
