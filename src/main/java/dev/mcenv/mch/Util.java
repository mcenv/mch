package dev.mcenv.mch;

import java.util.concurrent.TimeUnit;

final class Util {
  public static Integer parseIntOrNull(
    final String string
  ) {
    try {
      return Integer.parseInt(string);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  public static TimeUnit parseTimeUnit(final String string) {
    return switch (string) {
      case "ns" -> TimeUnit.NANOSECONDS;
      case "us" -> TimeUnit.MICROSECONDS;
      case "ms" -> TimeUnit.MILLISECONDS;
      case "s" -> TimeUnit.SECONDS;
      case "m" -> TimeUnit.MINUTES;
      default -> throw new IllegalArgumentException(String.format("Invalid time unit: '%s'", string));
    };
  }

  public static String abbreviate(final TimeUnit unit) {
    return switch (unit) {
      case NANOSECONDS -> "ns";
      case MICROSECONDS -> "us";
      case MILLISECONDS -> "ms";
      case SECONDS -> "s";
      case MINUTES -> "m";
      default -> throw new IllegalArgumentException(String.format("Invalid time unit: '%s'", unit));
    };
  }

  public static double convert(
    final double sourceDuration,
    final TimeUnit sourceUnit,
    final TimeUnit targetUnit
  ) {
    if (sourceUnit.ordinal() < targetUnit.ordinal()) {
      return sourceDuration / sourceUnit.convert(1, targetUnit);
    } else {
      return sourceDuration * targetUnit.convert(1, sourceUnit);
    }
  }

  public static String getCurrentJvm() {
    return ProcessHandle.current().info().command().orElseThrow();
  }
}
