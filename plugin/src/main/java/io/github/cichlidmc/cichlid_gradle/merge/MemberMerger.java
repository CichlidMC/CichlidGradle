package io.github.cichlidmc.cichlid_gradle.merge;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.*;
import java.util.function.Function;

public abstract class MemberMerger<T> {
    public final void merge(Map<MergeSource, ClassNode> classes, ClassNode merged) {
        SequencedMap<MemberKey, Map<MergeSource, T>> map = new LinkedHashMap<>();
        classes.forEach((source, node) -> {
            for (T member : this.getMembers(node)) {
                MemberKey key = this.makeKey(member);
                map.computeIfAbsent(key, $ -> new HashMap<>()).put(source, member);
            }
        });

        map.forEach((key, members) -> this.merge(key, members, merged));
    }

    private void merge(MemberKey key, Map<MergeSource, T> members, ClassNode merged) {
        if (members.size() == 1) {
            // exclusive
            Map.Entry<MergeSource, T> mapEntry = members.entrySet().iterator().next();
            MergeSource source = mapEntry.getKey();
            T member = mapEntry.getValue();
            AnnotationNode annotation = ClassMerger.makeDistAnnotation(source.dist);
            this.addAnnotation(member, annotation);
            this.addMember(merged, member);
            return;
        }

        // validate that merging is valid
        String qualifiedName = merged.name + '.' + key.name;
        AttributeMatcher<T> matcher = new AttributeMatcher<>(qualifiedName, members);
        this.assertCommonAttributesMatch(matcher);

        // all equivalent, grab from any source
        T member = members.values().iterator().next();
        this.addMember(merged, member);
    }

    protected abstract void addAnnotation(T member, AnnotationNode annotation);

    protected abstract void addMember(ClassNode clazz, T member);

    protected abstract List<T> getMembers(ClassNode clazz);

    protected abstract MemberKey makeKey(T member);

    protected abstract void assertCommonAttributesMatch(AttributeMatcher<T> matcher);

    public record MemberKey(String name, String desc) {
    }
}
