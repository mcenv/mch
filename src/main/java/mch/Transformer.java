package mch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class Transformer implements ClassFileTransformer {
    private final String args;

    public Transformer(String args) {
        this.args = args;
    }

    private static byte[] transform(byte[] classfileBuffer, Function<ClassVisitor, ClassVisitor> createClassVisitor) {
        var classReader = new ClassReader(classfileBuffer);
        var classWriter = new ClassWriter(classReader, 0);
        var classVisitor = createClassVisitor.apply(classWriter);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return switch (className) {
            case "net/minecraft/server/MinecraftServer" -> transform(classfileBuffer, ModNameTransformer::new);
            case "com/mojang/brigadier/CommandDispatcher" ->
                    transform(classfileBuffer, v -> new CommandInjector(v, args));
            default -> null;
        };
    }

    private static class ModNameTransformer extends ClassVisitor {
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

    private static class CommandInjector extends ClassVisitor {
        private final String benchmark;

        public CommandInjector(ClassVisitor classVisitor, String benchmark) {
            super(ASM9, classVisitor);
            this.benchmark = benchmark;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("<init>") && descriptor.equals("(Lcom/mojang/brigadier/tree/RootCommandNode;)V")) {
                return new MethodVisitor(ASM9, parent) {
                    @Override
                    public void visitInsn(int opcode) {
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
}
