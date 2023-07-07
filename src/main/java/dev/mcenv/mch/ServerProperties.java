package dev.mcenv.mch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import static dev.mcenv.mch.Util.parseIntOrNull;

record ServerProperties(
  String levelName
) {
  public static final String FUNCTION_PERMISSION_LEVEL_KEY = "function-permission-level";
  public static final int FUNCTION_PERMISSION_LEVEL_REQUIRED = 4;

  public static final String MAX_TICK_TIME_KEY = "max-tick-time";
  public static final int MAX_TICK_TIME_REQUIRED = -1;

  public static final String LEVEL_NAME_KEY = "level-name";
  public static final String LEVEL_NAME_DEFAULT = "world";

  public static final String INITIAL_ENABLED_PACKS = "initial-enabled-packs";
  public static final String INITIAL_ENABLED_PACKS_DEFAULT = "";

  public static final String INITIAL_DISABLED_PACKS = "initial-disabled-packs";
  public static final String INITIAL_DISABLED_PACKS_DEFAULT = "file/mch.zip";

  public static ServerProperties load() throws IOException {
    final var properties = new Properties();
    final var path = Paths.get("server.properties");

    String initialEnabledPacksRequired = INITIAL_ENABLED_PACKS_DEFAULT;
    String initialDisabledPacksRequired = INITIAL_DISABLED_PACKS_DEFAULT;

    if (Files.isRegularFile(path)) {
      try (final var in = Files.newInputStream(path)) {
        properties.load(in);
      }

      final var functionPermissionLevel = parseIntOrNull(properties.getProperty(FUNCTION_PERMISSION_LEVEL_KEY));
      if (functionPermissionLevel == null || functionPermissionLevel != FUNCTION_PERMISSION_LEVEL_REQUIRED) {
        System.out.printf("Overwriting %s in server.properties to %d\n", FUNCTION_PERMISSION_LEVEL_KEY, FUNCTION_PERMISSION_LEVEL_REQUIRED);
      }

      final var maxTickTime = parseIntOrNull(properties.getProperty(MAX_TICK_TIME_KEY));
      if (maxTickTime == null || maxTickTime != MAX_TICK_TIME_REQUIRED) {
        System.out.printf("Overwriting %s in server.properties to %d\n", MAX_TICK_TIME_KEY, MAX_TICK_TIME_REQUIRED);
      }

      final var initialEnabledPacks = properties.getProperty(INITIAL_ENABLED_PACKS).split(",");
      if (Arrays.asList(initialEnabledPacks).contains("file/mch.zip")) {
        initialEnabledPacksRequired = Arrays.stream(initialEnabledPacks).filter(s -> !"file/mch.zip".equals(s)).collect(Collectors.joining(","));
        System.out.printf("Overwriting %s in server.properties to %s\n", INITIAL_ENABLED_PACKS, initialEnabledPacksRequired);
      }

      final var initialDisabledPacks = properties.getProperty(INITIAL_DISABLED_PACKS).split(",");
      if (!Arrays.asList(initialDisabledPacks).contains("file/mch.zip")) {
        initialDisabledPacksRequired = properties.getProperty(INITIAL_DISABLED_PACKS) + ",file/mch.zip";
        System.out.printf("Overwriting %s in server.properties to %s\n", INITIAL_DISABLED_PACKS, initialDisabledPacksRequired);
      }
    } else {
      System.out.println("Creating server.properties");
    }

    properties.setProperty(FUNCTION_PERMISSION_LEVEL_KEY, String.valueOf(FUNCTION_PERMISSION_LEVEL_REQUIRED));
    properties.setProperty(MAX_TICK_TIME_KEY, String.valueOf(MAX_TICK_TIME_REQUIRED));
    properties.setProperty(INITIAL_ENABLED_PACKS, initialEnabledPacksRequired);
    properties.setProperty(INITIAL_DISABLED_PACKS, initialDisabledPacksRequired);

    try (final var out = Files.newOutputStream(path)) {
      properties.store(out, "Minecraft server properties");
    }

    return new ServerProperties(
      properties.getProperty(LEVEL_NAME_KEY, LEVEL_NAME_DEFAULT)
    );
  }
}
