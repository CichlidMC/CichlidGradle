package io.github.cichlidmc.cichlid_gradle.merge;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.List;

public class FieldMerger extends MemberMerger<FieldNode> {
    @Override
    protected void addAnnotation(FieldNode field, AnnotationNode annotation) {
        field.visibleAnnotations.add(annotation);
    }

    @Override
    protected void addMember(ClassNode clazz, FieldNode field) {
        clazz.fields.add(field);
    }

    @Override
    protected List<FieldNode> getMembers(ClassNode clazz) {
        return clazz.fields;
    }

    @Override
    protected MemberKey makeKey(FieldNode field) {
        return new MemberKey(field.name, field.desc);
    }

    @Override
    protected void assertCommonAttributesMatch(AttributeMatcher<FieldNode> matcher) {
        matcher.test(field -> field.access, "access");
        matcher.test(field -> field.signature, "signature");
        matcher.test(field -> field.value, "value");
        matcher.test(field -> field.visibleAnnotations, "visibleAnnotations");
        matcher.test(field -> field.invisibleAnnotations, "invisibleAnnotations");
        matcher.test(field -> field.visibleTypeAnnotations, "visibleTypeAnnotations");
        matcher.test(field -> field.invisibleTypeAnnotations, "invisibleTypeAnnotations");
        matcher.test(field -> field.attrs, "attrs");
    }
}
