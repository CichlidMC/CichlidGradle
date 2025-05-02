package fish.cichlidmc.cichlid_gradle.merge.element;

import fish.cichlidmc.cichlid_gradle.merge.ClassMerger;
import fish.cichlidmc.cichlid_gradle.merge.JarMerger;
import fish.cichlidmc.cichlid_gradle.merge.MergeSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

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

        // validate that merging is valid. Create a dummy class, add the member to it, check that bytes are equal
        List<ClassNode> dummyClasses = members.values().stream().map(member -> {
            ClassNode dummy = makeDummy();
            this.addMember(dummy, member);
            return dummy;
        }).toList();

        if (!JarMerger.allEqual(dummyClasses, MemberMerger::toBytes, Arrays::equals)) {
            String qualifiedName = merged.name + '.' + key.name;
            throw new IllegalStateException("Cannot merge member: " + qualifiedName);
        }

        // all equivalent, grab from any source
        T member = members.values().iterator().next();
        this.addMember(merged, member);
    }

    protected abstract void addAnnotation(T member, AnnotationNode annotation);

    protected abstract void addMember(ClassNode clazz, T member);

    protected abstract List<T> getMembers(ClassNode clazz);

    protected abstract MemberKey makeKey(T member);

    public record MemberKey(String name, String desc) {
    }

    private static ClassNode makeDummy() {
        ClassNode clazz = new ClassNode();
        clazz.version = Opcodes.V1_8;
        clazz.name = "fish/cichlidmc/Dummy";
        clazz.superName = "java/lang/Object";
        return clazz;
    }

    private static byte[] toBytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
