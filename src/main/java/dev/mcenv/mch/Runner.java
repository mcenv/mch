package dev.mcenv.mch;

import dev.mcenv.spy.Spy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class Runner {
  private final static String BASELINE = "mch:baseline";
  private final MchConfig mchConfig;
  private final String mcVersion;
  private final List<RunResult> runResults = new ArrayList<>();
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
      iterationRun(benchmark, Options.Iteration.Mode.PARSING);
    }

    for (final var benchmark : mchConfig.executeBenchmarks()) {
      iterationRun(benchmark, Options.Iteration.Mode.EXECUTE);
    }

    if (!mchConfig.functionBenchmarks().isEmpty()) {
      iterationRun(BASELINE, Options.Iteration.Mode.FUNCTION);
      for (final var entry : mchConfig.functionBenchmarks().entrySet()) {
        final var datapack = entry.getKey(); // TODO
        final var benchmarks = entry.getValue();
        for (final var benchmark : benchmarks) {
          iterationRun(benchmark, Options.Iteration.Mode.FUNCTION);
        }
      }
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
    final String benchmark,
    final Options.Iteration.Mode mode
  ) throws IOException, InterruptedException {
    final var scores = new ArrayList<Double>();
    for (var fork = 0; fork < mchConfig.forks(); ++fork) {
      try (final var server = new ServerSocket(0)) {
        final var thread = new Thread(() -> {
          try {
            final var client = server.accept();
            try (final var in = new ObjectInputStream(client.getInputStream())) {
              final var object = in.readObject();
              if (object instanceof Message.RunResult runResult) {
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
    runResults.add(new RunResult(benchmark, mode, scores));
  }
}
