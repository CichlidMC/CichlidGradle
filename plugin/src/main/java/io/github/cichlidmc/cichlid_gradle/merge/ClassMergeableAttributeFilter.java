package io.github.cichlidmc.cichlid_gradle.merge;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Discards all class attributes that are allowed to differ between client and server.
 * Includes fields, methods, inner classes, and implemented interfaces.
 */
public class ClassMergeableAttributeFilter extends ClassVisitor {
	public ClassMergeableAttributeFilter(ClassVisitor delegate) {
		super(Opcodes.ASM9, delegate);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, new String[0]);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return null;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
	}
}
