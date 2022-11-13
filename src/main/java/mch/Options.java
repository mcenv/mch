package mch;

@Keep
public sealed interface Options permits Options.Dry, Options.Iteration {
  static Options parse(
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
        Iteration.Mode.parse(args[7]),
        args[8]
      );
      default -> throw new IllegalArgumentException("Unknown tag: " + tag);
    };
  }

  @Keep
  final class Dry implements Options {
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
    Mode mode,
    String benchmark
  ) implements Options {
    @Override
    public String toString() {
      return String.format(
        "iteration,%d,%d,%d,%d,%d,%d,%s,%s",
        warmupIterations,
        measurementIterations,
        time,
        forks,
        fork,
        port,
        mode,
        benchmark
      );
    }

    @Keep
    public enum Mode {
      @Keep PARSING,
      @Keep EXECUTE;

      public static Mode parse(
        final String string
      ) {
        return switch (string) {
          case "PARSING" -> PARSING;
          case "EXECUTE" -> EXECUTE;
          default -> throw new IllegalArgumentException("Unknown mode: " + string);
        };
      }
    }
  }
}
