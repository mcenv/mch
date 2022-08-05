package mch.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ASM9;

public class ModNameTransformer extends ClassVisitor {
    public ModNameTransformer(ClassVisitor classVisitor) {
        super(ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (name.equals("getServerModName") && descriptor.equals("()Ljava/lang/String;")) {
            return new MethodVisitor(ASM9, parent) {
                @Override
                public void visitLdcInsn(Object value) {
                    super.visitLdcInsn("mch");
                }
            };
        } else {
            return parent;
        }
    }
}
