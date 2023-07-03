package dev.mcenv.mch;

import dev.mcenv.spy.Spy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

final class Runner {
  private final MchConfig mchConfig;
  private final String mcVersion;
  private final Map<String, RunResult> runResults = new LinkedHashMap<>();
  private final int total;
  private int done = 0;

  public Runner(
    final MchConfig mchConfig,
    final String mcVersion
  ) {
    this.mchConfig = mchConfig;
    this.mcVersion = mcVersion;
    total = (mchConfig.parsingBenchmarks().size() + mchConfig.executeBenchmarks().size() + mchConfig.functionBenchmarks().size()) * mchConfig.forks();
  }

  public void run() throws InterruptedException, IOException {
    dryRun();

    for (final var benchmark : mchConfig.parsingBenchmarks()) {
      iterationRun(Options.Iteration.Mode.PARSING, benchmark);
    }
    for (final var benchmark : mchConfig.executeBenchmarks()) {
      iterationRun(Options.Iteration.Mode.EXECUTE, benchmark);
    }
    for (final var benchmark : mchConfig.functionBenchmarks()) {
      iterationRun(Options.Iteration.Mode.FUNCTION, benchmark);
    }

    for (final var format : mchConfig.formats()) {
      format.write(mchConfig, mcVersion, runResults);
    }
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
}
