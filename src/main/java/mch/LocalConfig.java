package mch;

@Keep
public sealed interface LocalConfig permits LocalConfig.Dry, LocalConfig.Iteration {
  static LocalConfig parse(
    final String string
  ) {
    final var args = string.split(",");
    final var tag = args[0];
    return switch (tag) {
      case "dry" -> Dry.INSTANCE;
      case "iteration" -> new Iteration(
        Integer.parseInt(args[1]),
        Integer.parseInt(args[2]),
        Integer.parseInt(args[3]),
        Integer.parseInt(args[4]),
        Integer.parseInt(args[5]),
        Integer.parseInt(args[6]),
        args[7]
      );
      default -> throw new IllegalArgumentException("Unknown tag: " + tag);
    };
  }

  @Keep
  final class Dry implements LocalConfig {
    @Keep
    public static final Dry INSTANCE = new Dry();

    private Dry() {
    }

    @Override
    public String toString() {
      return "dry";
    }
  }

  @Keep
  record Iteration(
    int warmupIterations,
    int measurementIterations,
    int time,
    int forks,
    int fork,
    int port,
    String benchmark
  ) implements LocalConfig {
    @Override
    public String toString() {
      return String.format(
        "iteration,%d,%d,%d,%d,%d,%d,%s",
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
}
