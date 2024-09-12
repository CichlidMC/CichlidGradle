package io.github.cichlidmc.cichlid_gradle.merge;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassMergeableAttributeFilter extends ClassVisitor {
	public ClassMergeableAttributeFilter(ClassVisitor delegate) {
		super(Opcodes.ASM9, delegate);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return null;
	}
}
