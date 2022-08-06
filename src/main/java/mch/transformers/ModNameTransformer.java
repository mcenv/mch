package mch.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ASM9;

public final class ModNameTransformer extends ClassVisitor {
    public ModNameTransformer(final ClassVisitor classVisitor) {
        super(ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        final var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (name.equals("getServerModName") && descriptor.equals("()Ljava/lang/String;")) {
            return new MethodVisitor(ASM9, parent) {
                @Override
                public void visitLdcInsn(final Object value) {
                    super.visitLdcInsn("mch");
                }
            };
        } else {
            return parent;
        }
    }
}
