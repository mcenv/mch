package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
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

    try (final var out = new BufferedOutputStream(Files.newOutputStream(datapack))) {
      try (final var in = Main.class.getResourceAsStream("/mch.zip")) {
        Objects.requireNonNull(in).transferTo(out);
      }
    }

    try (final var server = new JarFile(mchConfig.mc())) {
      final var gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
      final var version = gson.fromJson(new String(server.getInputStream(new JarEntry("version.json")).readAllBytes(), StandardCharsets.UTF_8), Version.class);

      System.out.printf("Installing datapack in %s\n", datapack);
      return version.id;
    }
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
}
