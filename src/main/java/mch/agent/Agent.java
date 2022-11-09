package mch.agent;

import mch.Keep;
import mch.Options;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.function.Function;
import java.util.jar.JarFile;

@Keep
public final class Agent {
  @Keep
  public static void premain(
    final String args,
    final Instrumentation instrumentation
  ) throws IOException {
    System.out.println("Starting mch.agent.Agent");

    instrumentation.appendToSystemClassLoaderSearch(getBrigadier());
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
          case "com/mojang/brigadier/CommandDispatcher" -> transform(classfileBuffer, v -> new CommandInjector(v, Options.parse(args)));
          default -> null;
        };
      }
    });
  }

  private static JarFile getBrigadier() throws IOException {
    try (final var server = new JarFile("server.jar")) {
      try (final var libraries = new BufferedInputStream(server.getInputStream(server.getEntry("META-INF/libraries.list")))) {
        final var entries = new String(libraries.readAllBytes(), StandardCharsets.UTF_8).split("\n");
        final var brigadierPath =
          Arrays
            .stream(entries)
            .map(entry -> entry.split("\t")[2])
            .filter(path -> path.startsWith("com/mojang/brigadier/"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No brigadier was found"));

        try (final var brigadier = new BufferedInputStream(server.getInputStream(server.getEntry("META-INF/libraries/" + brigadierPath)))) {
          final var outPath = Paths.get("libraries", brigadierPath);
          Files.createDirectories(outPath.getParent());
          try (final var out = new BufferedOutputStream(Files.newOutputStream(outPath))) {
            brigadier.transferTo(out);
          }
        }

        return new JarFile("libraries/" + brigadierPath);
      }
    }
  }
}
