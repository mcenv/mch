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
          super.visitMaxs(config instanceof LocalConfig.Dry ? 4 : 10, 2);
        }

        @Override
        public void visitInsn(final int opcode) {
          if (opcode == RETURN) {
            visitVarInsn(ALOAD, 0);
            if (config instanceof LocalConfig.Dry) {
              visitFieldInsn(GETSTATIC, "mch/LocalConfig$Dry", "INSTANCE", "Lmch/LocalConfig$Dry;");
            } else if (config instanceof LocalConfig.Iteration iteration) {
              visitTypeInsn(NEW, "mch/LocalConfig$Iteration");
              visitInsn(DUP);
              visitLdcInsn(iteration.warmupIterations());
              visitLdcInsn(iteration.measurementIterations());
              visitLdcInsn(iteration.time());
              visitLdcInsn(iteration.forks());
              visitLdcInsn(iteration.fork());
              visitLdcInsn(iteration.port());
              visitLdcInsn(iteration.benchmark());
              visitMethodInsn(INVOKESPECIAL, "mch/LocalConfig$Iteration", "<init>", "(IIIIIILjava/lang/String;)V", false);
            }
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
