package mch.transformers;

import mch.LocalConfig;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public final class CommandInjector extends ClassVisitor {
  private final LocalConfig config;

  public CommandInjector(
    final ClassVisitor classVisitor,
    final LocalConfig config
  ) {
    super(ASM9, classVisitor);
    this.config = config;
  }

  @Override
  public MethodVisitor visitMethod(
    final int access,
    final String name,
    final String descriptor,
    final String signature,
    final String[] exceptions
  ) {
    final var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (name.equals("<init>") && descriptor.equals("(Lcom/mojang/brigadier/tree/RootCommandNode;)V")) {
      return new MethodVisitor(ASM9, parent) {
        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
          super.visitMaxs(10, 2);
        }

        @Override
        public void visitInsn(final int opcode) {
          if (opcode == RETURN) {
            visitVarInsn(ALOAD, 0);
            visitTypeInsn(NEW, "mch/LocalConfig");
            visitInsn(DUP);
            visitLdcInsn(config.warmupIterations());
            visitLdcInsn(config.measurementIterations());
            visitLdcInsn(config.time());
            visitLdcInsn(config.forks());
            visitLdcInsn(config.fork());
            visitLdcInsn(config.port());
            visitLdcInsn(config.benchmark());
            visitMethodInsn(INVOKESPECIAL, "mch/LocalConfig", "<init>", "(IIIIIILjava/lang/String;)V", false);
            visitMethodInsn(INVOKESTATIC, "mch/MchCommands", "register", "(Lcom/mojang/brigadier/CommandDispatcher;Lmch/LocalConfig;)V", false);
          }
          super.visitInsn(opcode);
        }
      };
    } else {
      return parent;
    }
  }
}
