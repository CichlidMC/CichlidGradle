package io.github.cichlidmc.cichlid_gradle.merge.element;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.List;

public class FieldMerger extends MemberMerger<FieldNode> {
    @Override
    protected void addAnnotation(FieldNode field, AnnotationNode annotation) {
        if (field.visibleAnnotations == null)
            field.visibleAnnotations = new ArrayList<>();
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
}
