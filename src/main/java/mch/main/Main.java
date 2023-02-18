package mch.main;

import com.google.gson.GsonBuilder;
import mch.Keep;
import mch.Options;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static mch.Util.*;

@Keep
public final class Main {
  @Keep
  public static void main(
    final String[] args
  ) throws InterruptedException, IOException {
    System.out.println("Starting mch.main.Main");

    if (!validateEula()) {
      System.out.println("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
      System.exit(1);
      return;
    }
    Datapack.install(ServerProperties.load());

    final var properties = MchProperties.load();

    dryRun(args);

    final var runResults = new LinkedHashMap<String, RunResult>();
    for (final var benchmark : properties.parsingBenchmarks()) {
      iterationRun(runResults, properties, Options.Iteration.Mode.PARSING, benchmark, args);
    }
    for (final var benchmark : properties.executeBenchmarks()) {
      iterationRun(runResults, properties, Options.Iteration.Mode.EXECUTE, benchmark, args);
    }
    writeResults(runResults, properties);
  }

  private static boolean validateEula() throws IOException {
    final var path = Paths.get("eula.txt");
    if (Files.exists(path) && Files.isRegularFile(path)) {
      try (final var in = Files.newInputStream(path)) {
        final var properties = new Properties();
        properties.load(in);
        return Boolean.parseBoolean(properties.getProperty("eula"));
      }
    } else {
      return false;
    }
  }

  private static void dryRun(
    final String[] args
  ) throws IOException, InterruptedException {
    final var options = Options.Dry.INSTANCE.toString();
    final var command = getCommand(options, args);
    final var builder = new ProcessBuilder(command);
    final var process = builder.start();
    process.getInputStream().transferTo(System.out);
    process.waitFor();
  }

  private static void iterationRun(
    final Map<String, RunResult> results,
    final MchProperties properties,
    final Options.Iteration.Mode mode,
    final String benchmark,
    final String[] args
  ) throws IOException, InterruptedException {
    for (var fork = 0; fork < properties.forks(); ++fork) {
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
              results.computeIfAbsent(benchmark, k -> new RunResult(new ArrayList<>(), mode)).scores().addAll(scores);
            }
          } catch (final IOException e) {
            throw new IllegalStateException(e);
          }
        });
        thread.start();

        final var port = server.getLocalPort();
        final var options = quote(new Options.Iteration(
          properties.warmupIterations(),
          properties.measurementIterations(),
          properties.time(),
          properties.forks(),
          fork,
          port,
          mode,
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
      final var java = getCurrentJvm();
      final var jar = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString();
      final var command = new ArrayList<String>();
      Collections.addAll(command, java, "-javaagent:" + jar + "=" + options, "-cp", quote(jar), "mch.fork.Fork", "nogui");
      Collections.addAll(command, args);
      return command;
    } catch (final URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void writeResults(
    final Map<String, RunResult> runResults,
    final MchProperties mchProperties
  ) throws IOException {
    final var unit = String.format("%s/op", abbreviate(mchProperties.timeUnit()));
    try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.json")))) {
      final String mchVersion;
      try (final var version = Main.class.getClassLoader().getResourceAsStream("version")) {
        mchVersion = new String(version.readAllBytes(), StandardCharsets.UTF_8).trim();
      }
      final var forks = mchProperties.forks();
      final var jvm = getCurrentJvm();
      final var jdkVersion = System.getProperty("java.version");
      final var vmName = System.getProperty("java.vm.name");
      final var vmVersion = System.getProperty("java.vm.version");
      final var warmupIterations = mchProperties.warmupIterations();
      final var warmupTime = String.format("%d %s", mchProperties.time(), abbreviate(TimeUnit.SECONDS));
      final var measurementIterations = mchProperties.measurementIterations();
      final var measurementTime = String.format("%d %s", mchProperties.time(), abbreviate(TimeUnit.SECONDS));
      final var entries = runResults
        .entrySet()
        .stream()
        .map(entry -> {
          final var benchmark = entry.getKey();
          final var runResult = entry.getValue();
          final var values = runResult.scores().stream().mapToDouble(x -> x).toArray();
          try {
            return new Results.Result(
              benchmark,
              runResult.mode().toString(),
              mchProperties.measurementIterations() * mchProperties.forks(),
              convert(Statistics.mean(values), TimeUnit.NANOSECONDS, mchProperties.timeUnit()),
              convert(Statistics.error(values), TimeUnit.NANOSECONDS, mchProperties.timeUnit()),
              unit
            );
          } catch (NotStrictlyPositiveException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();

      final var gson = new GsonBuilder().setPrettyPrinting().create();
      out.write(gson
        .toJson(new Results(
          mchVersion,
          forks,
          jvm,
          jdkVersion,
          vmName,
          vmVersion,
          warmupIterations,
          warmupTime,
          measurementIterations,
          measurementTime,
          entries
        ))
        .getBytes(StandardCharsets.UTF_8)
      );
    }
  }
}
