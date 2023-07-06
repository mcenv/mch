package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class MchDataPack {
  public final static String NAME = "file/mch.zip";

  public static String install(
    final MchConfig mchConfig,
    final ServerProperties serverProperties
  ) throws IOException {
    final var serverPath = Paths.get(mchConfig.mc());
    if (!Files.isRegularFile(serverPath)) {
      throw new IllegalStateException("No server.jar was found");
    }

    final var dataPack = Paths.get(serverProperties.levelName(), "datapacks", "mch.zip");
    Files.createDirectories(dataPack.getParent());

    final byte[] actualHash;
    if (Files.isRegularFile(dataPack)) {
      final MessageDigest digest;
      try {
        digest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
      try (final var in = new DigestInputStream(new BufferedInputStream(Files.newInputStream(dataPack)), digest)) {
        in.readAllBytes();
      }
      actualHash = digest.digest();
    } else {
      actualHash = new byte[0];
    }

    final byte[] expectedHash;
    try (final var in = Main.class.getResourceAsStream("/hash")) {
      expectedHash = Objects.requireNonNull(in).readAllBytes();
    }

    if (!Arrays.equals(actualHash, expectedHash)) {
      System.out.printf("Installing mch.zip in %s\n", dataPack);

      try (final var out = new BufferedOutputStream(Files.newOutputStream(dataPack))) {
        try (final var in = Main.class.getResourceAsStream("/mch.zip")) {
          Objects.requireNonNull(in).transferTo(out);
        }
      }
    }

    try (final var server = new JarFile(mchConfig.mc())) {
      final var gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
      final var version = gson.fromJson(new String(server.getInputStream(new JarEntry("version.json")).readAllBytes(), StandardCharsets.UTF_8), Version.class);

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
