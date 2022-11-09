package mch.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public record MchProperties(
  int warmupIterations,
  int measurementIterations,
  int time,
  int forks,
  String[] benchmarks
) {
  public static final String WARMUP_ITERATIONS_KEY = "warmup-iterations";
  public static final int WARMUP_ITERATIONS_DEFAULT = 5;

  public static final String MEASUREMENT_ITERATIONS_KEY = "measurement-iterations";
  public static final int MEASUREMENT_ITERATIONS_DEFAULT = 5;

  public static final String TIME_KEY = "time";
  public static final int TIME_DEFAULT = 10;

  public static final String FORKS_KEY = "forks";
  public static final int FORKS_DEFAULT = 5;

  public static final String BENCHMARKS = "benchmarks";
  public static final String BENCHMARKS_DEFAULT = ",";

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
    properties.putIfAbsent(BENCHMARKS, BENCHMARKS_DEFAULT);

    try (final var out = Files.newOutputStream(path)) {
      properties.store(out, null);
    }

    return new MchProperties(
      Integer.parseInt(properties.getProperty(WARMUP_ITERATIONS_KEY)),
      Integer.parseInt(properties.getProperty(MEASUREMENT_ITERATIONS_KEY)),
      Integer.parseInt(properties.getProperty(TIME_KEY)),
      Integer.parseInt(properties.getProperty(FORKS_KEY)),
      properties.getProperty(BENCHMARKS).split(",")
    );
  }
}
