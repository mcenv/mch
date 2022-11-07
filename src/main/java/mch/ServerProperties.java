package mch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static mch.Util.parseIntOrNull;

public record ServerProperties(
  String levelName
) {
  public static final String FUNCTION_PERMISSION_LEVEL_KEY = "function-permission-level";
  public static final int FUNCTION_PERMISSION_LEVEL_REQUIRED = 4;

  public static final String MAX_TICK_TIME_KEY = "max-tick-time";
  public static final int MAX_TICK_TIME_REQUIRED = -1;

  public static final String LEVEL_NAME_KEY = "level-name";
  public static final String LEVEL_NAME_DEFAULT = "world";

  public static ServerProperties load() throws IOException {
    final var properties = new Properties();
    final var path = Paths.get("server.properties");

    if (Files.exists(path) && Files.isRegularFile(path)) {
      try (final var in = Files.newInputStream(path)) {
        properties.load(in);
      }

      final var functionPermissionLevel = parseIntOrNull(properties.getProperty(FUNCTION_PERMISSION_LEVEL_KEY));
      if (functionPermissionLevel == null || functionPermissionLevel != FUNCTION_PERMISSION_LEVEL_REQUIRED) {
        properties.setProperty(FUNCTION_PERMISSION_LEVEL_KEY, String.valueOf(FUNCTION_PERMISSION_LEVEL_REQUIRED));
        System.out.printf("Overwrote %s in server.properties to %d\n", FUNCTION_PERMISSION_LEVEL_KEY, FUNCTION_PERMISSION_LEVEL_REQUIRED);
      }

      final var maxTickTime = parseIntOrNull(properties.getProperty(MAX_TICK_TIME_KEY));
      if (maxTickTime == null || maxTickTime != MAX_TICK_TIME_REQUIRED) {
        properties.setProperty(MAX_TICK_TIME_KEY, String.valueOf(MAX_TICK_TIME_REQUIRED));
        System.out.printf("Overwrote %s in server.properties to %d\n", MAX_TICK_TIME_KEY, MAX_TICK_TIME_REQUIRED);
      }
    }

    try (final var out = Files.newOutputStream(path)) {
      properties.store(out, "Minecraft server properties");
    }

    return new ServerProperties(
      properties.getProperty(LEVEL_NAME_KEY, LEVEL_NAME_DEFAULT)
    );
  }
}
