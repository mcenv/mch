package mch;

@Keep
public record LocalConfig(
  int warmupIterations,
  int measurementIterations,
  int time,
  int forks,
  int fork,
  int port,
  String benchmark
) {
  public static final int DRY_RUN_FORK = -1;

  public static LocalConfig parse(
    final String string
  ) {
    final var args = string.split(",");
    return new LocalConfig(
      Integer.parseInt(args[0]),
      Integer.parseInt(args[1]),
      Integer.parseInt(args[2]),
      Integer.parseInt(args[3]),
      Integer.parseInt(args[4]),
      Integer.parseInt(args[5]),
      args[6]
    );
  }

  @Override
  public String toString() {
    return String.format(
      "%d,%d,%d,%d,%d,%d,%s",
      warmupIterations,
      measurementIterations,
      time,
      forks,
      fork,
      port,
      benchmark
    );
  }
}
