package fish.cichlidmc.cichlid_gradle.merge.element;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class MethodMerger extends MemberMerger<MethodNode> {
    @Override
    protected void addAnnotation(MethodNode method, AnnotationNode annotation) {
        if (method.visibleAnnotations == null)
            method.visibleAnnotations = new ArrayList<>();
        method.visibleAnnotations.add(annotation);
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
}
