package mch.main;

import com.google.gson.FieldNamingPolicy;
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

    try {
      validateEula();
      Datapack.install(ServerProperties.load());

      final var mchConfig = loadConfig(args);

      dryRun(mchConfig);

      final var runResults = new LinkedHashMap<String, RunResult>();

      for (final var benchmark : mchConfig.parsingBenchmarks()) {
        iterationRun(runResults, mchConfig, Options.Iteration.Mode.PARSING, benchmark);
      }

      for (final var benchmark : mchConfig.executeBenchmarks()) {
        iterationRun(runResults, mchConfig, Options.Iteration.Mode.EXECUTE, benchmark);
      }

      writeResults(runResults, mchConfig);
    } catch (final IllegalStateException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void validateEula() throws IOException {
    final var path = Paths.get("eula.txt");
    if (Files.exists(path) && Files.isRegularFile(path)) {
      try (final var in = Files.newInputStream(path)) {
        final var properties = new Properties();
        properties.load(in);
        if (!Boolean.parseBoolean(properties.getProperty("eula"))) {
          throw new IllegalStateException("You need to agree to the EULA in order to run the server");
        }
      }
    } else {
      throw new IllegalStateException("No eula.txt was found");
    }
  }

  private static MchConfig loadConfig(
    final String[] args
  ) throws IOException {
    return new GsonBuilder()
      .registerTypeAdapter(MchConfig.class, new MchConfig.Deserializer(args))
      .create()
      .fromJson(Files.newBufferedReader(Paths.get("mch-config.json")), MchConfig.class);
  }

  private static void dryRun(
    final MchConfig mchConfig
  ) throws IOException, InterruptedException {
    final var options = Options.Dry.INSTANCE.toString();
    final var command = getCommand(options, mchConfig);
    final var builder = new ProcessBuilder(command);
    final var process = builder.start();
    process.getInputStream().transferTo(System.out);
    process.waitFor();
  }

  private static void iterationRun(
    final Map<String, RunResult> runResults,
    final MchConfig mchConfig,
    final Options.Iteration.Mode mode,
    final String benchmark
  ) throws IOException, InterruptedException {
    for (var fork = 0; fork < mchConfig.forks(); ++fork) {
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
              runResults.computeIfAbsent(benchmark, k -> new RunResult(new ArrayList<>(), mode)).scores().addAll(scores);
            }
          } catch (final IOException e) {
            throw new IllegalStateException(e);
          }
        });
        thread.start();

        final var port = server.getLocalPort();
        final var options = new Options.Iteration(
          mchConfig.warmupIterations(),
          mchConfig.measurementIterations(),
          mchConfig.time(),
          mchConfig.forks(),
          fork,
          port,
          mode,
          benchmark
        ).toString();
        final var command = getCommand(options, mchConfig);
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
    final MchConfig mchConfig
  ) {
    try {
      final var java = getCurrentJvm();
      final var jar = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString();
      final var command = new ArrayList<String>();
      command.add(java);
      command.addAll(mchConfig.jvmArgs());
      Collections.addAll(command,
        "-javaagent:" + jar + "=" + options,
        "-Dmch.server=" + mchConfig.mc(),
        "-cp",
        jar,
        "mch.fork.Fork"
      );
      command.addAll(mchConfig.mcArgs());
      return command;
    } catch (final URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void writeResults(
    final Map<String, RunResult> runResults,
    final MchConfig mchConfig
  ) throws IOException {
    final var unit = String.format("%s/op", abbreviate(mchConfig.timeUnit()));
    try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.json")))) {
      final String mchVersion;
      try (final var version = Main.class.getClassLoader().getResourceAsStream("version")) {
        mchVersion = new String(version.readAllBytes(), StandardCharsets.UTF_8).trim();
      }
      final var forks = mchConfig.forks();
      final var jvm = getCurrentJvm();
      final var jvmArgs = mchConfig.jvmArgs();
      final var jdkVersion = System.getProperty("java.version");
      final var vmName = System.getProperty("java.vm.name");
      final var vmVersion = System.getProperty("java.vm.version");
      final var mc = mchConfig.mc();
      final var mcArgs = mchConfig.mcArgs();
      final var warmupIterations = mchConfig.warmupIterations();
      final var warmupTime = String.format("%d %s", mchConfig.time(), abbreviate(TimeUnit.SECONDS));
      final var measurementIterations = mchConfig.measurementIterations();
      final var measurementTime = String.format("%d %s", mchConfig.time(), abbreviate(TimeUnit.SECONDS));
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
              mchConfig.measurementIterations() * mchConfig.forks(),
              convert(Statistics.mean(values), TimeUnit.NANOSECONDS, mchConfig.timeUnit()),
              convert(Statistics.error(values), TimeUnit.NANOSECONDS, mchConfig.timeUnit()),
              unit
            );
          } catch (NotStrictlyPositiveException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();

      final var gson = new GsonBuilder().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
      out.write(gson
        .toJson(new Results(
          mchVersion,
          forks,
          jvm,
          jvmArgs,
          jdkVersion,
          vmName,
          vmVersion,
          mc,
          mcArgs,
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
