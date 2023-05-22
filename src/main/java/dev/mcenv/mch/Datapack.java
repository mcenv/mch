package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Datapack {
  public static String install(
    final MchConfig mchConfig,
    final ServerProperties serverProperties
  ) throws IOException {
    final var serverPath = Paths.get(mchConfig.mc());
    if (!Files.exists(serverPath) || !Files.isRegularFile(serverPath)) {
      throw new IllegalStateException("No server.jar was found");
    }

    final var datapack = Paths.get(serverProperties.levelName(), "datapacks", "mch.zip");
    Files.createDirectories(datapack.getParent());

    try (final var out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(datapack)))) {
      final var gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

      final Version version;
      try (final var server = new JarFile(mchConfig.mc())) {
        version = gson.fromJson(new String(server.getInputStream(new JarEntry("version.json")).readAllBytes(), StandardCharsets.UTF_8), Version.class);
        writeEntry(out, "pack.mcmeta", gson.toJson(new PackMetadata(new PackMetadataSection("", version.packVersion.data))));
      }

      writeEntry(out, "data/minecraft/tags/functions/load.json", gson.toJson(new Tag(
        "mch:pre",
        "#mch:setup.trial",
        "mch:start",
        "#mch:teardown.trial",
        "mch:post"
      )));

      final var emptyTag = gson.toJson(new Tag());
      writeEntry(out, "data/mch/tags/functions/setup.json", emptyTag);
      writeEntry(out, "data/mch/tags/functions/setup.trial.json", emptyTag);
      writeEntry(out, "data/mch/tags/functions/setup.iteration.json", emptyTag);
      writeEntry(out, "data/mch/tags/functions/teardown.trial.json", emptyTag);
      writeEntry(out, "data/mch/tags/functions/teardown.iteration.json", emptyTag);

      writeEntry(out, "data/mch/functions/pre.mcfunction", """
        gamerule maxCommandChainLength 2147483647""");

      writeEntry(out, "data/mch/functions/start.mcfunction", """
        mch:start""");

      writeEntry(out, "data/mch/functions/loop.mcfunction", """
        mch:loop""");

      writeEntry(out, "data/mch/functions/post.mcfunction", """
        mch:post
        stop""");

      System.out.printf("Installing datapack in %s\n", datapack);
      return version.id;
    }
  }

  private static void writeEntry(
    final ZipOutputStream out,
    final String name,
    final String content
  ) throws IOException {
    out.putNextEntry(new ZipEntry(name));
    out.write(content.getBytes(StandardCharsets.UTF_8));
  }

  private record Version(
    String id,
    PackVersion packVersion
  ) {
  }

  private record PackVersion(
    int data
  ) {
  }

  private record PackMetadata(
    PackMetadataSection pack
  ) {
  }

  private record PackMetadataSection(
    String description,
    int packFormat
  ) {
  }

  private record Tag(
    String... values
  ) {
  }
}