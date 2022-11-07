package mch;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static mch.ServerProperties.*;
import static mch.Util.bytesToDouble;
import static mch.Util.quote;

@Keep
public final class Main {
  @Keep
  public static void main(
    final String[] args
  ) throws InterruptedException, IOException {
    System.out.println("Starting mch.Main");

    {
      final var serverProperties = ServerProperties.load();
      validateServerProperties(serverProperties);
      installDatapack(serverProperties);
    }

    final var mchProperties = MchProperties.load();

    dryRun(args);

    final var results = new LinkedHashMap<String, Collection<Double>>();
    for (final var benchmark : mchProperties.benchmarks()) {
      iterationRun(results, mchProperties, benchmark, args);
    }

    dumpResults(results, mchProperties);
  }

  public static void validateServerProperties(
    final ServerProperties serverProperties
  ) throws IOException {
    final var properties = new Properties();

    if (serverProperties.functionPermissionLevel() == null || serverProperties.functionPermissionLevel() != FUNCTION_PERMISSION_LEVEL_REQUIRED) {
      properties.setProperty(FUNCTION_PERMISSION_LEVEL_KEY, String.valueOf(FUNCTION_PERMISSION_LEVEL_REQUIRED));
      System.out.printf("Overwrote %s in server.properties to %d\n", FUNCTION_PERMISSION_LEVEL_KEY, FUNCTION_PERMISSION_LEVEL_REQUIRED);
    }

    if (serverProperties.maxTickTime() == null || serverProperties.maxTickTime() != MAX_TICK_TIME_REQUIRED) {
      properties.setProperty(MAX_TICK_TIME_KEY, String.valueOf(MAX_TICK_TIME_REQUIRED));
      System.out.printf("Overwrote %s in server.properties to %d\n", MAX_TICK_TIME_KEY, MAX_TICK_TIME_REQUIRED);
    }

    try (final var out = Files.newOutputStream(Paths.get("server.properties"))) {
      properties.store(out, null);
    }
  }

  private static void installDatapack(
    final ServerProperties serverProperties
  ) throws IOException {
    final var datapack = Paths.get(serverProperties.levelName(), "datapacks", "mch.zip");
    if (Files.notExists(datapack)) {
      Files.createDirectories(datapack.getParent());
      try (final var out = new BufferedOutputStream(Files.newOutputStream(datapack))) {
        try (final var in = Main.class.getResourceAsStream("/mch.zip")) {
          Objects.requireNonNull(in).transferTo(out);
        }
      }
      System.out.printf("Installed datapack in %s\n", datapack);
    }
  }

  private static void dryRun(
    final String[] args
  ) throws IOException, InterruptedException {
    final var options = LocalConfig.Dry.INSTANCE.toString();
    final var command = getCommand(options, args);
    final var builder = new ProcessBuilder(command);
    final var process = builder.start();
    process.getInputStream().transferTo(System.out);
    process.waitFor();
  }

  private static void iterationRun(
    final Map<String, Collection<Double>> results,
    final MchProperties config,
    final String benchmark,
    final String[] args
  ) throws IOException, InterruptedException {
    for (var fork = 0; fork < config.forks(); ++fork) {
      try (final var server = new ServerSocket(0)) {
        final var thread = new Thread(() -> {
          try {
            final var client = server.accept();
            try (final var in = client.getInputStream()) {
              final var scores = new ArrayList<Double>();
              final var buffer = new byte[Double.BYTES];
              while (in.readNBytes(buffer, 0, Double.BYTES) == Double.BYTES) {
                scores.add(bytesToDouble(buffer));
              }
              results.computeIfAbsent(benchmark, k -> new ArrayList<>()).addAll(scores);
            }
          } catch (final IOException e) {
            throw new IllegalStateException(e);
          }
        });
        thread.start();

        final var port = server.getLocalPort();
        final var options = quote(new LocalConfig.Iteration(
          config.warmupIterations(),
          config.measurementIterations(),
          config.time(),
          config.forks(),
          fork,
          port,
          benchmark
        ).toString());
        final var command = getCommand(options, args);
        final var builder = new ProcessBuilder(command);
        final var process = builder.start();
        process.getInputStream().transferTo(System.out);
        process.waitFor();

        thread.join();
      }
    }
  }

  private static List<String> getCommand(
    final String options,
    final String[] args
  ) {
    try {
      final var java = ProcessHandle.current().info().command().orElseThrow();
      final var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
      final var command = new ArrayList<String>();
      Collections.addAll(command, java, "-javaagent:" + jar + "=" + options, "-cp", jar, "mch.Fork", "nogui");
      Collections.addAll(command, args);
      return command;
    } catch (final URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void dumpResults(
    final Map<String, Collection<Double>> results,
    final MchProperties config
  ) throws IOException {
    try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.json")))) {
      out.write('[');

      {
        var i = 0;
        for (final var entry : results.entrySet()) {
          final var benchmark = entry.getKey();
          final var values = entry.getValue().stream().mapToDouble(x -> x).toArray();
          if (i++ != 0) {
            out.write(',');
          }
          try {
            out.write(
              String.format(
                """
                  \n  { "benchmark": "%s", "count": %d, "score": %f, "error": %f, "unit": "%s" }""",
                benchmark,
                config.measurementIterations() * config.forks(),
                Statistics.mean(values),
                Statistics.error(values),
                "ns/op"
              ).getBytes(StandardCharsets.UTF_8)
            );
          } catch (NotStrictlyPositiveException ignored) {
            out.write(
              String.format(
                """
                  \n  { "benchmark": "%s" }""",
                benchmark
              ).getBytes(StandardCharsets.UTF_8)
            );
          }
        }
      }

      out.write('\n');
      out.write(']');
      out.write('\n');
    }
  }
}
