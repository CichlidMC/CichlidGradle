package io.github.cichlidmc.cichlid_gradle.merge;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class AttributeMatcher<T> {
    private final String name;
    private final Map<MergeSource, T> members;

    public AttributeMatcher(String name, Map<MergeSource, T> members) {
        this.name = name;
        this.members = members;
    }

    public void test(Function<T, Object> function, String attributeName) {
        if (!JarMerger.allEqual(this.members.values(), function, AttributeMatcher::equals)) {
            throw new IllegalStateException("Attribute mismatch between sources: " + attributeName + " in " + this.name);
        }
    }

    // equals impls for ASM classes

    private static boolean equals(Object a, Object b) {
        if (a == null) return b == null;
        if (b == null) return false;

        if (a instanceof List<?> listA && b instanceof List<?> listB) {
            return equalLists(listA, listB);
        } else if (a instanceof Object[] arrayA && b instanceof Object[] arrayB) {
            return equalLists(Arrays.asList(arrayA), Arrays.asList(arrayB));
        } else if (a instanceof ModuleNode moduleA && b instanceof ModuleNode moduleB) {
            return equalModules(moduleA, moduleB);
        } else if (a instanceof AnnotationNode annotationA && b instanceof AnnotationNode annotationB) {
            return equalAnnotations(annotationA, annotationB);
        } else if (a instanceof Attribute attributeA && b instanceof Attribute attributeB) {
            return equalAttributes(attributeA, attributeB);
        } else if (a instanceof InnerClassNode classA && b instanceof InnerClassNode classB) {
            return equalInnerClasses(classA, classB);
        } else if (a instanceof RecordComponentNode recordA && b instanceof RecordComponentNode recordB) {
            return equalRecordComponents(recordA, recordB);
        }

        return Objects.equals(a, b);
    }

    private static boolean equalLists(List<?> a, List<?> b) {

    }

    private static boolean equalModules(ModuleNode a, ModuleNode b) {

    }

    private static boolean equalAnnotations(AnnotationNode a, AnnotationNode b) {
        if (a instanceof TypeAnnotationNode typeA && b instanceof TypeAnnotationNode typeB) {

        }
    }

    private static boolean equalAttributes(Attribute a, Attribute b) {

    }

    private static boolean equalInnerClasses(InnerClassNode a, InnerClassNode b) {

    }

    private static boolean equalRecordComponents(RecordComponentNode a, RecordComponentNode b) {

    }
}
