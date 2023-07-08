package dev.mcenv.mch;

sealed interface Options permits Options.Setup, Options.Iteration {
  static Options parse(
    final String string
  ) {
    final var args = string.split(",");
    final var tag = args[0];
    return switch (tag) {
      case "setup" -> Setup.INSTANCE;
      case "iteration" -> new Iteration(
        Boolean.parseBoolean(args[1]),
        Boolean.parseBoolean(args[2]),
        Integer.parseInt(args[3]),
        Integer.parseInt(args[4]),
        Integer.parseInt(args[5]),
        Integer.parseInt(args[6]),
        Integer.parseInt(args[7]),
        Integer.parseInt(args[8]),
        Float.parseFloat(args[9]),
        Iteration.Mode.parse(args[10]),
        args[11]
      );
      default -> throw new IllegalArgumentException("Unknown tag: " + tag);
    };
  }

  final class Setup implements Options {
    public static final Setup INSTANCE = new Setup();

    private Setup() {
    }

    @Override
    public String toString() {
      return "setup";
    }
  }

  record Iteration(
    boolean autoStart,
    boolean lastInGroup,
    int warmupIterations,
    int measurementIterations,
    int time,
    int forks,
    int fork,
    int port,
    float progress,
    Mode mode,
    String benchmark
  ) implements Options {
    @Override
    public String toString() {
      return String.format(
        "iteration,%b,%b,%d,%d,%d,%d,%d,%d,%f,%s,%s",
        autoStart,
        lastInGroup,
        warmupIterations,
        measurementIterations,
        time,
        forks,
        fork,
        port,
        progress,
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
