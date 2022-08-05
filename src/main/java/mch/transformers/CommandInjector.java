package mch.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class CommandInjector extends ClassVisitor {
    private final String benchmark;

    public CommandInjector(final ClassVisitor classVisitor, final String benchmark) {
        super(ASM9, classVisitor);
        this.benchmark = benchmark;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        final var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (name.equals("<init>") && descriptor.equals("(Lcom/mojang/brigadier/tree/RootCommandNode;)V")) {
            return new MethodVisitor(ASM9, parent) {
                @Override
                public void visitInsn(final int opcode) {
                    if (opcode == RETURN) {
                        visitVarInsn(ALOAD, 0);
                        visitLdcInsn(benchmark);
                        visitMethodInsn(INVOKESTATIC, "mch/MchCommands", "register", "(Lcom/mojang/brigadier/CommandDispatcher;Ljava/lang/String;)V", false);
                    }
                    super.visitInsn(opcode);
                }
            };
        } else {
            return parent;
        }
    }
}
