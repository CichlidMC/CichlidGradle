package io.github.cichlidmc.cichlid_gradle.merge;

import io.github.cichlidmc.annotations.Dist;
import io.github.cichlidmc.annotations.Distribution;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        
        AttributeMatcher<ClassNode> matcher = new AttributeMatcher<>(names.getFirst(), classes);

        matcher.test(node -> node.version, "version");
        matcher.test(node -> node.access, "access");
        matcher.test(node -> node.signature, "signature");
        matcher.test(node -> node.superName, "superName");
        matcher.test(node -> node.interfaces, "interfaces");
        matcher.test(node -> node.sourceFile, "sourceFile");
        matcher.test(node -> node.sourceDebug, "sourceDebug");
        matcher.test(node -> node.module, "module");
        matcher.test(node -> node.outerClass, "outerClass");
        matcher.test(node -> node.outerMethod, "outerMethod");
        matcher.test(node -> node.outerMethodDesc, "outerMethodDesc");
        matcher.test(node -> node.visibleAnnotations, "visibleAnnotations");
        matcher.test(node -> node.invisibleAnnotations, "invisibleAnnotations");
        matcher.test(node -> node.visibleTypeAnnotations, "visibleTypeAnnotations");
        matcher.test(node -> node.invisibleTypeAnnotations, "invisibleTypeAnnotations");
        matcher.test(node -> node.attrs, "attrs");
        matcher.test(node -> node.innerClasses, "innerClasses");
        matcher.test(node -> node.nestHostClass, "nestHostClass");
        matcher.test(node -> node.nestMembers, "nestMembers");
        matcher.test(node -> node.permittedSubclasses, "permittedSubclasses");
        matcher.test(node -> node.recordComponents, "recordComponents");
    }

    public static void copyExclusiveClass(Path path, Path dest, Dist dist) throws IOException {
        ClassNode node = readClass(path);
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
