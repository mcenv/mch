package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import dev.mcenv.spy.Spy;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static dev.mcenv.mch.Util.*;

final class Runner {
  private final MchConfig mchConfig;
  private final String mcVersion;
  private final Map<String, RunResult> runResults = new LinkedHashMap<>();

  public Runner(
    final MchConfig mchConfig,
    final String mcVersion
  ) {
    this.mchConfig = mchConfig;
    this.mcVersion = mcVersion;
  }

  public void run() throws InterruptedException, IOException {
    dryRun();

    for (final var benchmark : mchConfig.parsingBenchmarks()) {
      iterationRun(Options.Iteration.Mode.PARSING, benchmark);
    }

    for (final var benchmark : mchConfig.executeBenchmarks()) {
      iterationRun(Options.Iteration.Mode.EXECUTE, benchmark);
    }

    writeResults();
  }

  private void dryRun() throws IOException, InterruptedException {
    final var options = Options.Dry.INSTANCE.toString();
    final var process = Spy.create(
      Paths.get(mchConfig.mc()),
      MchCommands.class,
      options,
      mchConfig.mcArgs().toArray(new String[0]),
      mchConfig.jvmArgs().toArray(new String[0])
    ).start();
    process.getInputStream().transferTo(System.out);
    process.waitFor();
  }

  private void iterationRun(
    final Options.Iteration.Mode mode,
    final String benchmark
  ) throws IOException, InterruptedException {
    var done = 0;
    final var total = (mchConfig.parsingBenchmarks().size() + mchConfig.executeBenchmarks().size()) * mchConfig.forks();
    for (var fork = 0; fork < mchConfig.forks(); ++fork) {
      try (final var server = new ServerSocket(0)) {
        final var thread = new Thread(() -> {
          try {
            final var client = server.accept();
            try (final var in = new ObjectInputStream(client.getInputStream())) {
              final var object = in.readObject();
              if (object instanceof Message.RunResult runResult) {
                final var scores = runResults.computeIfAbsent(benchmark, k -> new RunResult(new ArrayList<>(), mode)).scores();
                for (final var score : runResult.scores()) {
                  scores.add(score);
                }
              }
            }
          } catch (final IOException | ClassNotFoundException e) {
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
          done++,
          total,
          mode,
          benchmark
        ).toString();
        final var process = Spy.create(
          Paths.get(mchConfig.mc()),
          MchCommands.class,
          options,
          mchConfig.mcArgs().toArray(new String[0]),
          mchConfig.jvmArgs().toArray(new String[0])
        ).start();
        process.getInputStream().transferTo(System.out);
        process.waitFor();

        thread.join();
      }
    }
  }

  private void writeResults() throws IOException {
    final var unit = String.format("%s/op", abbreviate(mchConfig.timeUnit()));
    try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.json")))) {
      final String mchVersion;
      try (final var version = Main.class.getClassLoader().getResourceAsStream("version")) {
        mchVersion = new String(Objects.requireNonNull(version).readAllBytes(), StandardCharsets.UTF_8).trim();
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
          mcVersion,
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
