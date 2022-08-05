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

    public Transformer(final Map<String, Function<ClassVisitor, ClassVisitor>> transformers) {
        this.transformers = transformers;
    }

    private static byte[] transform(final byte[] classfileBuffer, final Function<ClassVisitor, ClassVisitor> createClassVisitor) {
        final var classReader = new ClassReader(classfileBuffer);
        final var classWriter = new ClassWriter(classReader, 0);
        final var classVisitor = createClassVisitor.apply(classWriter);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        return transform(classfileBuffer, transformers.get(className));
    }
}
