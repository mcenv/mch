package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import dev.mcenv.spy.Spy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Runner {
  private final static String BASELINE = "mch:baseline";
  private final static Pattern RESOURCE_LOCATION = Pattern.compile("^([a-z0-9_.-]+)/functions/([a-z0-9/._-]+)\\.mcfunction$");
  private final static FileSystem FILE_SYSTEM = FileSystems.getDefault();
  private final static PathMatcher MCFUNCTION_MATCHER = FILE_SYSTEM.getPathMatcher("glob:*/functions/**.mcfunction");

  private final MchConfig mchConfig;
  private final String levelName;
  private final String mcVersion;
  private final Collection<RunResult> runResults = new ArrayList<>();
  private int total = 1; // for baseline
  private int done = 0;

  public Runner(
    final MchConfig mchConfig,
    final String levelName,
    final String mcVersion
  ) {
    this.mchConfig = mchConfig;
    this.levelName = levelName;
    this.mcVersion = mcVersion;
  }

  public void run() throws InterruptedException, IOException {
    dryRun();

    final var benchmarkDataPacks = collectBenchmarkDataPacks();
    final var benchmarksByDataPack = new LinkedHashMap<String, Collection<String>>();

    total += mchConfig.parsingBenchmarks().size();
    total += mchConfig.executeBenchmarks().size();
    for (final var dataPack : mchConfig.functionBenchmarks()) {
      if (benchmarkDataPacks.contains(dataPack)) {
        final var benchmarks = collectBenchmarkFunctions(dataPack);
        total += benchmarks.size();
        benchmarksByDataPack.put(dataPack, benchmarks);
      } else {
        System.err.println("Warning: Data pack " + dataPack + " is not for benchmarking");
      }
    }
    total *= mchConfig.forks();

    modifyLevelStorage(benchmarkDataPacks, null);

    for (final var benchmark : mchConfig.parsingBenchmarks()) {
      iterationRun(benchmark, Options.Iteration.Mode.PARSING, null);
    }

    for (final var benchmark : mchConfig.executeBenchmarks()) {
      iterationRun(benchmark, Options.Iteration.Mode.EXECUTE, null);
    }

    if (!mchConfig.functionBenchmarks().isEmpty()) {
      iterationRun(BASELINE, Options.Iteration.Mode.FUNCTION, null);

      for (final var dataPack : mchConfig.functionBenchmarks()) {
        modifyLevelStorage(benchmarkDataPacks, dataPack);
        final var benchmarks = benchmarksByDataPack.get(dataPack);
        for (final var benchmark : benchmarks) {
          iterationRun(benchmark, Options.Iteration.Mode.FUNCTION, dataPack);
        }
      }
    }

    for (final var format : mchConfig.formats()) {
      format.write(mchConfig, mcVersion, runResults);
    }
  }

  private Set<String> collectBenchmarkDataPacks() throws IOException {
    final var gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

    final var dataPacksRoot = Paths.get(levelName, "datapacks");
    try (final var dataPacks = Files.list(dataPacksRoot)) {
      return dataPacks
        .map(dataPackRoot -> dataPackRoot.resolve("pack.mcmeta"))
        .filter(Files::isRegularFile)
        .flatMap(packMetadataPath -> {
          try (final var reader = Files.newBufferedReader(packMetadataPath)) {
            final var packMetadata = gson.fromJson(reader, PackMetadata.class);
            if (packMetadata.pack.mch) {
              return Stream.of("file/" + dataPacksRoot.relativize(packMetadataPath.getParent()));
            } else {
              return Stream.of();
            }
          } catch (IOException e) {
            return Stream.of();
          }
        })
        .collect(Collectors.toSet());
    }
  }

  private Collection<String> collectBenchmarkFunctions(
    final String dataPack
  ) throws IOException {
    final var functions = new ArrayList<String>();
    final var root = Paths.get(levelName, "datapacks", dataPack.substring("file/".length()), "data");
    final var visitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        final var relativePath = root.relativize(file);
        if (MCFUNCTION_MATCHER.matches(relativePath)) {
          try (final var reader = new BufferedReader(new FileReader(file.toFile()))) {
            final var line = reader.readLine();
            if ("# @benchmark".equals(line)) {
              final var invariantSeparatorsPathString = relativePath.toString().replace(FILE_SYSTEM.getSeparator(), "/");
              final var matcher = RESOURCE_LOCATION.matcher(invariantSeparatorsPathString);
              if (matcher.matches()) {
                final var namespace = matcher.group(1);
                final var path = matcher.group(2);
                functions.add(namespace + ':' + path);
              }
            }
          }
        }
        return FileVisitResult.CONTINUE;
      }
    };
    Files.walkFileTree(root, visitor);
    return functions;
  }

  private void modifyLevelStorage(
    final Set<String> benchmarkDataPacks,
    final String enabledDataPack
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
    if (enabledDataPack != null) {
      enabledDataPacks.add(new Nbt.String(enabledDataPack));
    }

    final var disabledDataPacks = new LinkedHashSet<Nbt.String>();
    for (final var element : ((Nbt.List) dataPacks.elements().get("Disabled")).elements()) {
      disabledDataPacks.add(((Nbt.String) element));
    }
    for (final var benchmarkDataPack : benchmarkDataPacks) {
      disabledDataPacks.add(new Nbt.String(benchmarkDataPack));
    }
    if (enabledDataPack != null) {
      disabledDataPacks.remove(new Nbt.String(enabledDataPack));
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

  private record PackMetadata(
    PackMetadataSection pack
  ) {
  }

  private record PackMetadataSection(
    int packFormat,
    Boolean mch
  ) {
  }
}
