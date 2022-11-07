package mch;

import mch.transformers.CommandInjector;
import mch.transformers.ModNameTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.function.Function;
import java.util.jar.JarFile;

@Keep
public final class Agent {
  @Keep
  public static void premain(
    final String args,
    final Instrumentation instrumentation
  ) throws IOException {
    System.out.println("Starting mch.Agent");

    instrumentation.appendToSystemClassLoaderSearch(new JarFile("libraries/com/mojang/brigadier/1.0.18/brigadier-1.0.18.jar"));
    instrumentation.addTransformer(new ClassFileTransformer() {
      private static byte[] transform(
        final byte[] classfileBuffer,
        final Function<ClassVisitor, ClassVisitor> createClassVisitor
      ) {
        final var classReader = new ClassReader(classfileBuffer);
        final var classWriter = new ClassWriter(classReader, 0);
        final var classVisitor = createClassVisitor.apply(classWriter);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
      }

      @Override
      public byte[] transform(
        final ClassLoader loader,
        final String className, Class<?> classBeingRedefined,
        final ProtectionDomain protectionDomain,
        final byte[] classfileBuffer
      ) {
        return switch (className) {
          case "net/minecraft/server/MinecraftServer" -> transform(classfileBuffer, ModNameTransformer::new);
          case "com/mojang/brigadier/CommandDispatcher" -> transform(classfileBuffer, v -> new CommandInjector(v, LocalConfig.parse(args)));
          default -> null;
        };
      }
    });
  }
}
