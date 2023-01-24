package mch.main;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import mch.Keep;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Datapack {
  private static final int PACK_FORMAT = 11; // TODO

  public static void install(
    final ServerProperties serverProperties
  ) throws IOException {
    final var datapack = Paths.get(serverProperties.levelName(), "datapacks", "mch.zip");
    Files.createDirectories(datapack.getParent());

    try (final var out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(datapack)))) {
      final var gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

      writeEntry(out, "pack.mcmeta", gson.toJson(new PackMetadata(new PackMetadataSection("", PACK_FORMAT))));

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
    }

    System.out.printf("Installed datapack in %s\n", datapack);
  }

  private static void writeEntry(
    final ZipOutputStream out,
    final String name,
    final String content
  ) throws IOException {
    out.putNextEntry(new ZipEntry(name));
    out.write(content.getBytes(StandardCharsets.UTF_8));
  }

  @Keep
  private record PackMetadata(
    @Keep PackMetadataSection pack
  ) {
  }

  @Keep
  private record PackMetadataSection(
    @Keep String description,
    @Keep int packFormat
  ) {
  }

  @Keep
  private record Tag(
    @Keep String... values
  ) {
  }
}
