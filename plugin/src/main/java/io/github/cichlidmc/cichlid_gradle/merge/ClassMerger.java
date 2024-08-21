package io.github.cichlidmc.cichlid_gradle.merge;

import io.github.cichlidmc.annotations.Dist;
import io.github.cichlidmc.annotations.Distribution;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ClassMerger {
    public static void run(Path path, Path other, Path merged, Dist dist) throws IOException {
        // class has already been merged
        if (Files.exists(merged))
            return;

        // check for exclusivity
        if (Files.exists(path) && !Files.exists(other)) {
            copyExclusiveClass(path, merged, dist);
            return;
        }

        // merge fields, methods, and constructors
        // note: this code path shouldn't be hit after 1.17
        ClassNode left = readClass(path);
        ClassNode right = readClass(other);

        assertCommonFieldsMatch(left, right, path.getFileName().toString());
        ClassNode mergedNode = new ClassNode();
        // base merged on left, shouldn't matter
        left.accept(mergedNode);

        mergedNode.fields.clear();
        mergedNode.methods.clear();
        mergeFields(left, right, mergedNode);
        mergeMethods(left, right, mergedNode);

        writeClass(mergedNode, merged);
    }

    private static void assertCommonFieldsMatch(ClassNode left, ClassNode right, String className) {
        // these should always match, if they don't something is very wrong
        assertEqual(left.version, right.version, "version", className);
        assertEqual(left.access, right.access, "access", className);
        assertEqual(left.name, right.name, "name", className);
        assertEqual(left.signature, right.signature, "signature", className);
        assertEqual(left.superName, right.superName, "superName", className);
        assertEqual(left.interfaces, right.interfaces, "interfaces", className);
        assertEqual(left.sourceFile, right.sourceFile, "sourceFile", className);
        assertEqual(left.sourceDebug, right.sourceDebug, "sourceDebug", className);
        assertEqual(left.module, right.module, "module", className);
        assertEqual(left.outerClass, right.outerClass, "outerClass", className);
        assertEqual(left.outerMethod, right.outerMethod, "outerMethod", className);
        assertEqual(left.outerMethodDesc, right.outerMethodDesc, "outerMethodDesc", className);
        assertEqual(left.visibleAnnotations, right.visibleAnnotations, "visibleAnnotations", className);
        assertEqual(left.invisibleAnnotations, right.invisibleAnnotations, "invisibleAnnotations", className);
        assertEqual(left.visibleTypeAnnotations, right.visibleTypeAnnotations, "visibleTypeAnnotations", className);
        assertEqual(left.invisibleTypeAnnotations, right.invisibleTypeAnnotations, "invisibleTypeAnnotations", className);
        assertEqual(left.attrs, right.attrs, "attrs", className);
        assertEqual(left.innerClasses, right.innerClasses, "innerClasses", className);
        assertEqual(left.nestHostClass, right.nestHostClass, "nestHostClass", className);
        assertEqual(left.nestMembers, right.nestMembers, "nestMembers", className);
        assertEqual(left.permittedSubclasses, right.permittedSubclasses, "permittedSubclasses", className);
        assertEqual(left.recordComponents, right.recordComponents, "recordComponents", className);
    }

    private static void mergeFields(ClassNode left, ClassNode right, ClassNode merged) {
    }

    private static void mergeMethods(ClassNode left, ClassNode right, ClassNode merged) {
    }

    private static void copyExclusiveClass(Path path, Path dest, Dist dist) throws IOException {
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

    private static AnnotationNode makeDistAnnotation(Dist dist) {
        AnnotationNode node = new AnnotationNode(Type.getDescriptor(Distribution.class));
        node.visitEnum("value", Type.getDescriptor(Dist.class), dist.name());
        return node;
    }

    private static Dist opposite(Dist dist) {
        return switch (dist) {
            case CLIENT -> Dist.SERVER;
            case SERVER -> Dist.CLIENT;
            case BUNDLER -> throw new IllegalArgumentException();
        };
    }

    private static void assertEqual(Object left, Object right, String name, String className) {
        if (!Objects.equals(left, right)) {
            throw new IllegalStateException("Mismatch between client and server class values of '" + name + "': " + left + ", " + right + " (class: " + className + ')');
        }
    }
}
