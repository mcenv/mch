package dev.mcenv.mch;

import dev.mcenv.spy.Spy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

final class Runner {
  private final static String BASELINE = "mch:baseline";
  private final MchConfig mchConfig;
  private final String levelName;
  private final String mcVersion;
  private final Collection<RunResult> runResults = new ArrayList<>();
  private final int total;
  private int done = 0;

  public Runner(
    final MchConfig mchConfig,
    final String levelName,
    final String mcVersion
  ) {
    this.mchConfig = mchConfig;
    this.levelName = levelName;
    this.mcVersion = mcVersion;
    total = (mchConfig.parsingBenchmarks().size() + mchConfig.executeBenchmarks().size() + mchConfig.functionBenchmarks().size()) * mchConfig.forks();
  }

  public void run() throws InterruptedException, IOException {
    dryRun();

    final var benchmarkDataPacks = mchConfig.functionBenchmarks().keySet();
    modifyLevelStorage(benchmarkDataPacks, null);

    for (final var benchmark : mchConfig.parsingBenchmarks()) {
      iterationRun(benchmark, Options.Iteration.Mode.PARSING, null);
    }

    for (final var benchmark : mchConfig.executeBenchmarks()) {
      iterationRun(benchmark, Options.Iteration.Mode.EXECUTE, null);
    }

    if (!mchConfig.functionBenchmarks().isEmpty()) {
      iterationRun(BASELINE, Options.Iteration.Mode.FUNCTION, null);

      for (final var entry : mchConfig.functionBenchmarks().entrySet()) {
        final var dataPack = entry.getKey();
        modifyLevelStorage(benchmarkDataPacks, dataPack);
        for (final var benchmark : entry.getValue()) {
          iterationRun(benchmark, Options.Iteration.Mode.FUNCTION, dataPack);
        }
      }
    }

    for (final var format : mchConfig.formats()) {
      format.write(mchConfig, mcVersion, runResults);
    }
  }

  private void modifyLevelStorage(
    final Set<String> benchmarkDataPacks,
    final String enabledDatapack
  ) throws IOException {
    final var levelDat = Paths.get(levelName, "level.dat");
    final var levelStorage = Nbt.read(levelDat);
    final var data = (Nbt.Compound) levelStorage.elements().get("Data");
    final var dataPacks = (Nbt.Compound) data.elements().get("DataPacks");

    final var enabledDataPacks = new LinkedHashSet<Nbt.String>();
    for (final var element : ((Nbt.List) dataPacks.elements().get("Enabled")).elements()) {
      if (!benchmarkDataPacks.contains(((Nbt.String) element).value())) {
        enabledDataPacks.add((Nbt.String) element);
      }
    }
    if (enabledDatapack != null) {
      enabledDataPacks.add(new Nbt.String(enabledDatapack));
    }

    final var disabledDataPacks = new LinkedHashSet<Nbt.String>();
    for (final var element : ((Nbt.List) dataPacks.elements().get("Disabled")).elements()) {
      disabledDataPacks.add(((Nbt.String) element));
    }
    for (final var benchmarkDataPack : benchmarkDataPacks) {
      disabledDataPacks.add(new Nbt.String(benchmarkDataPack));
    }
    if (enabledDatapack != null) {
      disabledDataPacks.remove(new Nbt.String(enabledDatapack));
    }

    dataPacks.elements().put("Enabled", new Nbt.List(enabledDataPacks.stream().toList()));
    dataPacks.elements().put("Disabled", new Nbt.List(disabledDataPacks.stream().toList()));

    System.out.println("Overwriting Data.DataPacks in level.dat");
    Nbt.write(levelStorage, levelDat);
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
    final Options.Iteration.Mode mode,
    final String group
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
    runResults.add(new RunResult(group, benchmark, mode, scores));
  }
}
