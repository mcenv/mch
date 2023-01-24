package mch.main;

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
      writeEntry(out, "pack.mcmeta", String.format("""
        {"pack":{"description":"","pack_format":%d}}""", PACK_FORMAT));

      writeEntry(out, "data/minecraft/tags/functions/load.json", """
        {"values": ["mch:pre","#mch:setup.trial","mch:start","#mch:teardown.trial","mch:post"]}""");

      writeEntry(out, "data/mch/tags/functions/setup.json", """
        {"values":[]}""");

      writeEntry(out, "data/mch/tags/functions/setup.trial.json", """
        {"values":[]}""");

      writeEntry(out, "data/mch/tags/functions/setup.iteration.json", """
        {"values":[]}""");

      writeEntry(out, "data/mch/tags/functions/teardown.trial.json", """
        {"values":[]}""");

      writeEntry(out, "data/mch/tags/functions/teardown.iteration.json", """
        {"values":[]}""");

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
}
