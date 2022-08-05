package mch.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.function.Function;

public class Transformer implements ClassFileTransformer {
    private final Map<String, Function<ClassVisitor, ClassVisitor>> transformers;

    public Transformer(Map<String, Function<ClassVisitor, ClassVisitor>> transformers) {
        this.transformers = transformers;
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
        return transform(classfileBuffer, transformers.get(className));
    }
}
