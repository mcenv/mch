package mch;

import java.util.concurrent.TimeUnit;

public final class Util {
  public static String quote(
    final String string
  ) {
    return '"' + string + '"';
  }

  public static Integer parseIntOrNull(
    final String string
  ) {
    try {
      return Integer.parseInt(string);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  public static byte[] doubleToBytes(
    final double d
  ) {
    final var l = Double.doubleToLongBits(d);
    return new byte[]{
      (byte) (l >>> 56),
      (byte) (l >>> 48),
      (byte) (l >>> 40),
      (byte) (l >>> 32),
      (byte) (l >>> 24),
      (byte) (l >>> 16),
      (byte) (l >>> 8),
      (byte) l
    };
  }

  public static double bytesToDouble(
    final byte[] bytes
  ) {
    return Double.longBitsToDouble(
      ((long) bytes[0] << 56) +
        ((long) (bytes[1] & 255) << 48) +
        ((long) (bytes[2] & 255) << 40) +
        ((long) (bytes[3] & 255) << 32) +
        ((long) (bytes[4] & 255) << 24) +
        ((bytes[5] & 255) << 16) +
        ((bytes[6] & 255) << 8) +
        (bytes[7] & 255)
    );
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
