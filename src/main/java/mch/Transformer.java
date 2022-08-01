package mch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.ASM9;

public class Transformer implements ClassFileTransformer {
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
            default -> null;
        };
    }

    static class ModNameTransformer extends ClassVisitor {
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
}
