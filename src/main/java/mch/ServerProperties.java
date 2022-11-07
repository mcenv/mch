package mch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static mch.Util.parseIntOrNull;

public record ServerProperties(
  Integer functionPermissionLevel,
  Integer maxTickTime,
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

    final Integer functionPermissionLevel;
    final Integer maxTickTime;
    final String levelName;

    if (Files.exists(path) && Files.isRegularFile(path)) {
      try (final var in = Files.newInputStream(path)) {
        properties.load(in);
      }

      functionPermissionLevel = parseIntOrNull(properties.getProperty(FUNCTION_PERMISSION_LEVEL_KEY));
      maxTickTime = parseIntOrNull(properties.getProperty(MAX_TICK_TIME_KEY));
      levelName = properties.getProperty(LEVEL_NAME_KEY, LEVEL_NAME_DEFAULT);
    } else {
      functionPermissionLevel = null;
      maxTickTime = null;
      levelName = LEVEL_NAME_DEFAULT;
    }

    return new ServerProperties(
      functionPermissionLevel,
      maxTickTime,
      levelName
    );
  }
}
