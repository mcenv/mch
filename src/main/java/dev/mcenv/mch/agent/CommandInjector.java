package dev.mcenv.mch.agent;

import dev.mcenv.mch.Options;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public final class CommandInjector extends ClassVisitor {
  private final Options options;

  public CommandInjector(
    final ClassVisitor classVisitor,
    final Options options
  ) {
    super(ASM9, classVisitor);
    this.options = options;
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
          super.visitMaxs(options instanceof Options.Dry ? 4 : 11, 2);
        }

        @Override
        public void visitInsn(final int opcode) {
          if (opcode == RETURN) {
            visitVarInsn(ALOAD, 0);
            if (options instanceof Options.Dry) {
              visitFieldInsn(GETSTATIC, "dev/mcenv/mch/Options$Dry", "INSTANCE", "Ldev/mcenv/mch/Options$Dry;");
            } else if (options instanceof Options.Iteration iteration) {
              visitTypeInsn(NEW, "dev/mcenv/mch/Options$Iteration");
              visitInsn(DUP);
              visitLdcInsn(iteration.warmupIterations());
              visitLdcInsn(iteration.measurementIterations());
              visitLdcInsn(iteration.time());
              visitLdcInsn(iteration.forks());
              visitLdcInsn(iteration.fork());
              visitLdcInsn(iteration.port());
              visitFieldInsn(GETSTATIC, "dev/mcenv/mch/Options$Iteration$Mode", iteration.mode().name(), "Ldev/mcenv/mch/Options$Iteration$Mode;");
              visitLdcInsn(iteration.benchmark());
              visitMethodInsn(INVOKESPECIAL, "dev/mcenv/mch/Options$Iteration", "<init>", "(IIIIIILdev/mcenv/mch/Options$Iteration$Mode;Ljava/lang/String;)V", false);
            }
            visitMethodInsn(INVOKESTATIC, "dev/mcenv/mch/agent/MchCommands", "register", "(Lcom/mojang/brigadier/CommandDispatcher;Ldev/mcenv/mch/Options;)V", false);
          }
          super.visitInsn(opcode);
        }
      };
    } else {
      return parent;
    }
  }
}
