package dev.mcenv.mch;

sealed interface Options permits Options.Dry, Options.Iteration {
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
        Integer.parseInt(args[7]),
        Integer.parseInt(args[8]),
        Iteration.Mode.parse(args[9]),
        args[10]
      );
      default -> throw new IllegalArgumentException("Unknown tag: " + tag);
    };
  }

  final class Dry implements Options {
    public static final Dry INSTANCE = new Dry();

    private Dry() {
    }

    @Override
    public String toString() {
      return "dry";
    }
  }

  record Iteration(
    int warmupIterations,
    int measurementIterations,
    int time,
    int forks,
    int fork,
    int port,
    int done,
    int total,
    Mode mode,
    String benchmark
  ) implements Options {
    @Override
    public String toString() {
      return String.format(
        "iteration,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s",
        warmupIterations,
        measurementIterations,
        time,
        forks,
        fork,
        port,
        done,
        total,
        mode,
        benchmark
      );
    }

    public enum Mode {
      PARSING("parsing"),
      EXECUTE("execute"),
      FUNCTION("function");

      private final String name;

      Mode(
        final String name
      ) {
        this.name = name;
      }

      public static Mode parse(
        final String string
      ) {
        return switch (string) {
          case "parsing" -> PARSING;
          case "execute" -> EXECUTE;
          case "function" -> FUNCTION;
          default -> throw new IllegalArgumentException("Unknown mode: " + string);
        };
      }

      @Override
      public String toString() {
        return name;
      }
    }
  }
}
