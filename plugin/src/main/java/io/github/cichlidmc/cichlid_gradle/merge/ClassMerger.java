package io.github.cichlidmc.cichlid_gradle.merge;

import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.distmarker.Dist;
import io.github.cichlidmc.distmarker.Distribution;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClassMerger {
    private static final FieldMerger fieldMerger = new FieldMerger();
    private static final MethodMerger methodMerger = new MethodMerger();

    public static void mergeClass(Map<MergeSource, Path> sources, Path dest) throws IOException {
        // merge fields, methods, and constructors
        // note: this code path shouldn't be hit after 1.17
        Map<MergeSource, ClassNode> classes = new HashMap<>();
        for (Map.Entry<MergeSource, Path> entry : sources.entrySet()) {
            classes.put(entry.getKey(), readClass(entry.getValue()));
        }

        assertCommonAttributesMatch(classes);
        ClassNode mergedNode = new ClassNode();

        // doesn't matter which it's based on
        ClassNode base = classes.values().iterator().next();
        base.accept(mergedNode);

        mergedNode.fields.clear();
        mergedNode.methods.clear();
        fieldMerger.merge(classes, mergedNode);
        methodMerger.merge(classes, mergedNode);

        writeClass(mergedNode, dest);
    }

    private static void assertCommonAttributesMatch(Map<MergeSource, ClassNode> classes) {
        // these should always match, if they don't something is very wrong

        List<String> names = classes.values().stream().map(node -> node.name).distinct().toList();
        if (names.size() != 1) {
            throw new IllegalStateException("Trying to merge classes with different names: " + names);
        }

        // convert each class to bytes, filtering out mergeable attributes
        // implementing an equals method for every needed field type is actually hell

		if (!JarMerger.allEqual(classes.values(), ClassMerger::toCommonBytes, Arrays::equals)) {
			throw new IllegalStateException("Common attribute mismatch for class " + names.getFirst());
		}
    }

    private static byte[] toCommonBytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(new ClassMergeableAttributeFilter(writer));
        return writer.toByteArray();
    }

    public static void copyExclusiveClass(Path path, Path dest, Dist dist) throws IOException {
        ClassNode node = readClass(path);
        if (node.visibleAnnotations == null)
            node.visibleAnnotations = new ArrayList<>();
        node.visibleAnnotations.add(makeDistAnnotation(dist));
        writeClass(node, dest);
    }

    private static ClassNode readClass(Path path) throws IOException {
        ClassReader reader = new ClassReader(Files.newInputStream(path));
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        return node;
    }

    private static void writeClass(ClassNode node, Path path) throws IOException {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        FileUtils.ensureCreated(path);
        Files.write(path, bytes);
    }

    public static AnnotationNode makeDistAnnotation(Dist dist) {
        AnnotationNode node = new AnnotationNode(Type.getDescriptor(Distribution.class));
        node.visitEnum("value", Type.getDescriptor(Dist.class), dist.name());
        return node;
    }
}
