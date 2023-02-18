package mch.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static mch.Util.abbreviate;
import static mch.Util.parseTimeUnit;

public record MchProperties(
  int warmupIterations,
  int measurementIterations,
  int time,
  int forks,
  TimeUnit timeUnit,
  String[] jvmArgs,
  String[] mcArgs,
  String[] parsingBenchmarks,
  String[] executeBenchmarks
) {
  public static final String WARMUP_ITERATIONS_KEY = "warmup-iterations";
  public static final int WARMUP_ITERATIONS_DEFAULT = 5;

  public static final String MEASUREMENT_ITERATIONS_KEY = "measurement-iterations";
  public static final int MEASUREMENT_ITERATIONS_DEFAULT = 5;

  public static final String TIME_KEY = "time";
  public static final int TIME_DEFAULT = 10;

  public static final String FORKS_KEY = "forks";
  public static final int FORKS_DEFAULT = 5;

  public static final String TIME_UNIT_KEY = "time-unit";
  public static final TimeUnit TIME_UNIT_DEFAULT = TimeUnit.SECONDS;

  public static final String JVM_ARGS_KEY = "jvm-args";
  public static final String JVM_ARGS_DEFAULT = ",";

  public static final String MC_ARGS_KEY = "mc-args";
  public static final String MC_ARGS_DEFAULT = "nogui";

  public static final String PARSING_BENCHMARKS = "parsing-benchmarks";
  public static final String PARSING_BENCHMARKS_DEFAULT = ",";

  public static final String EXECUTE_BENCHMARKS = "execute-benchmarks";
  public static final String EXECUTE_BENCHMARKS_DEFAULT = ",";

  public static MchProperties load() throws IOException {
    final var properties = new Properties();
    final var path = Paths.get("mch.properties");

    if (Files.exists(path) && Files.isRegularFile(path)) {
      try (final var in = Files.newInputStream(path)) {
        properties.load(in);
      }
    }

    properties.putIfAbsent(WARMUP_ITERATIONS_KEY, String.valueOf(WARMUP_ITERATIONS_DEFAULT));
    properties.putIfAbsent(MEASUREMENT_ITERATIONS_KEY, String.valueOf(MEASUREMENT_ITERATIONS_DEFAULT));
    properties.putIfAbsent(TIME_KEY, String.valueOf(TIME_DEFAULT));
    properties.putIfAbsent(FORKS_KEY, String.valueOf(FORKS_DEFAULT));
    properties.putIfAbsent(TIME_UNIT_KEY, abbreviate(TIME_UNIT_DEFAULT));
    properties.putIfAbsent(JVM_ARGS_KEY, JVM_ARGS_DEFAULT);
    properties.putIfAbsent(MC_ARGS_KEY, MC_ARGS_DEFAULT);
    properties.putIfAbsent(PARSING_BENCHMARKS, PARSING_BENCHMARKS_DEFAULT);
    properties.putIfAbsent(EXECUTE_BENCHMARKS, EXECUTE_BENCHMARKS_DEFAULT);

    try (final var out = Files.newOutputStream(path)) {
      properties.store(out, null);
    }

    return new MchProperties(
      Integer.parseInt(properties.getProperty(WARMUP_ITERATIONS_KEY)),
      Integer.parseInt(properties.getProperty(MEASUREMENT_ITERATIONS_KEY)),
      Integer.parseInt(properties.getProperty(TIME_KEY)),
      Integer.parseInt(properties.getProperty(FORKS_KEY)),
      parseTimeUnit(properties.getProperty(TIME_UNIT_KEY)),
      properties.getProperty(JVM_ARGS_KEY).split(","),
      properties.getProperty(MC_ARGS_KEY).split(","),
      properties.getProperty(PARSING_BENCHMARKS).split(","),
      properties.getProperty(EXECUTE_BENCHMARKS).split(",")
    );
  }
}
