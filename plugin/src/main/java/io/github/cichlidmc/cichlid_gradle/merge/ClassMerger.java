package io.github.cichlidmc.cichlid_gradle.merge;

import io.github.cichlidmc.annotations.Dist;
import io.github.cichlidmc.annotations.Distribution;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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


    }

    private static void copyExclusiveClass(Path path, Path dest, Dist dist) throws IOException {
        ClassReader reader = new ClassReader(Files.newInputStream(path));
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        AnnotationNode annotation = new AnnotationNode(Type.getDescriptor(Distribution.class));
        annotation.visitEnum("value", Type.getDescriptor(Dist.class), "CLIENT");
        node.visibleAnnotations.add(annotation);

        ClassWriter writer = new ClassWriter(0);
        byte[] bytes = writer.toByteArray();
    }

    private static Dist opposite(Dist dist) {
        return switch (dist) {
            case CLIENT -> Dist.SERVER;
            case SERVER -> Dist.CLIENT;
            case BUNDLER -> throw new IllegalArgumentException();
        };
    }
}
