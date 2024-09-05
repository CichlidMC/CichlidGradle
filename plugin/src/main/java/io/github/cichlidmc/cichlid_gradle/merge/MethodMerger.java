package io.github.cichlidmc.cichlid_gradle.merge;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class MethodMerger extends MemberMerger<MethodNode> {
    @Override
    protected void addAnnotation(MethodNode method, AnnotationNode annotation) {
        method.invisibleAnnotations.add(annotation);
    }

    @Override
    protected void addMember(ClassNode clazz, MethodNode method) {
        clazz.methods.add(method);
    }

    @Override
    protected List<MethodNode> getMembers(ClassNode clazz) {
        return clazz.methods;
    }

    @Override
    protected MemberKey makeKey(MethodNode method) {
        return new MemberKey(method.name, method.desc);
    }

    @Override
    protected void assertCommonAttributesMatch(AttributeMatcher<MethodNode> matcher) {
        matcher.test(method -> method.access, "access");
        matcher.test(method -> method.signature, "signature");
        matcher.test(method -> method.exceptions, "exceptions");
        matcher.test(method -> method.parameters, "parameters");
        matcher.test(method -> method.visibleAnnotations, "visibleAnnotations");
        matcher.test(method -> method.invisibleAnnotations, "invisibleAnnotations");
        matcher.test(method -> method.visibleTypeAnnotations, "visibleTypeAnnotations");
        matcher.test(method -> method.invisibleTypeAnnotations, "invisibleTypeAnnotations");
        matcher.test(method -> method.attrs, "attrs");
        matcher.test(method -> method.annotationDefault, "annotationDefault");
        matcher.test(method -> method.visibleAnnotableParameterCount, "visibleAnnotableParameterCount");
        matcher.test(method -> method.visibleParameterAnnotations, "visibleParameterAnnotations");
        matcher.test(method -> method.invisibleAnnotableParameterCount, "invisibleAnnotableParameterCount");
        matcher.test(method -> method.invisibleParameterAnnotations, "invisibleParameterAnnotations");
        matcher.test(method -> method.instructions, "instructions");
        matcher.test(method -> method.tryCatchBlocks, "tryCatchBlocks");
        matcher.test(method -> method.maxStack, "maxStack");
        matcher.test(method -> method.maxLocals, "maxLocals");
        matcher.test(method -> method.localVariables, "localVariables");
        matcher.test(method -> method.visibleLocalVariableAnnotations, "visibleLocalVariableAnnotations");
        matcher.test(method -> method.invisibleLocalVariableAnnotations, "invisibleLocalVariableAnnotations");
    }
}
