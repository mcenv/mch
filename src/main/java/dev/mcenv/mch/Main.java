package dev.mcenv.mch;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public final class Main {
  public static void main(
    final String[] args
  ) throws InterruptedException, IOException {
    System.out.println("Starting dev.mcenv.mch.Main");

    try {
      validateEula();
      final var mchConfig = loadConfig(args);
      final var serverProperties = ServerProperties.load();
      final var mcVersion = DataPack.install(mchConfig, serverProperties);
      new Runner(mchConfig, serverProperties.levelName(), mcVersion).run();
    } catch (final IllegalStateException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void validateEula() throws IOException {
    final var path = Paths.get("eula.txt");
    if (Files.isRegularFile(path)) {
      try (final var in = Files.newInputStream(path)) {
        final var properties = new Properties();
        properties.load(in);
        if (!Boolean.parseBoolean(properties.getProperty("eula"))) {
          throw new IllegalStateException("You need to agree to the EULA in order to run the server");
        }
      }
    } else {
      throw new IllegalStateException("No eula.txt was found");
    }
  }

  private static MchConfig loadConfig(
    final String[] args
  ) throws IOException {
    final var mchConfigPath = Paths.get("mch-config.json");
    if (Files.isRegularFile(mchConfigPath)) {
      return new GsonBuilder()
        .registerTypeAdapter(MchConfig.class, new MchConfig.Deserializer(args))
        .create()
        .fromJson(Files.newBufferedReader(mchConfigPath), MchConfig.class);
    } else {
      return new MchConfig.Builder(args).build();
    }
  }
}
