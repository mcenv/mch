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
  private final static String FILE_PREFIX = "file/";
  private final static String MCH_GROUP = "mch";
  private final static String MCH_BASELINE = "mch:baseline";
  private final static Pattern RESOURCE_LOCATION = Pattern.compile("^([a-z0-9_.-]+)/functions/([a-z0-9/._-]+)\\.mcfunction$");
  private final static FileSystem FILE_SYSTEM = FileSystems.getDefault();
  private final static PathMatcher MCFUNCTION_MATCHER = FILE_SYSTEM.getPathMatcher("glob:*/functions/**.mcfunction");

  private final MchConfig mchConfig;
  private final String levelName;
  private final String mcVersion;
  private final Collection<RunResult> runResults = new ArrayList<>();
  private int done = 0;
  private int total = 1; // 1 for baseline

  public Runner(
    final MchConfig mchConfig,
    final String levelName,
    final String mcVersion
  ) {
    this.mchConfig = mchConfig;
    this.levelName = levelName;
    this.mcVersion = mcVersion;
  }

  private static Thread runIterationThread(
    final ServerSocket server,
    final ArrayList<Double> scores
  ) {
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
    return thread;
  }

  public void run() throws InterruptedException, IOException {
    setupRun(true);

    final var benchmarksByDataPack = new LinkedHashMap<String, List<String>>();
    final var benchmarkDataPacks = collectBenchmarkDataPacks();
    final var prefixedBenchmarkDataPacks = benchmarkDataPacks.stream()
      .map(dataPack -> FILE_PREFIX + dataPack)
      .collect(Collectors.toSet());

    total += mchConfig.parsingBenchmarks().size();
    total += mchConfig.executeBenchmarks().size();
    for (final var benchmarkDataPack : benchmarkDataPacks) {
      final var benchmarks = collectBenchmarkFunctions(benchmarkDataPack);
      total += benchmarks.size();
      benchmarksByDataPack.put(FILE_PREFIX + benchmarkDataPack, benchmarks);
    }
    total *= mchConfig.forks();

    modifyLevelStorage(prefixedBenchmarkDataPacks, null);

    for (final var benchmark : mchConfig.parsingBenchmarks()) {
      iterationRun(benchmark, Options.Iteration.Mode.PARSING, MCH_GROUP, false);
    }

    for (final var benchmark : mchConfig.executeBenchmarks()) {
      iterationRun(benchmark, Options.Iteration.Mode.EXECUTE, MCH_GROUP, false);
    }

    if (!benchmarksByDataPack.isEmpty()) {
      iterationRun(MCH_BASELINE, Options.Iteration.Mode.FUNCTION, MCH_GROUP, false);

      for (final var entry : benchmarksByDataPack.entrySet()) {
        final var dataPack = entry.getKey();
        final var benchmarks = entry.getValue();
        modifyLevelStorage(prefixedBenchmarkDataPacks, dataPack);
        setupRun(false);
        for (var i = 0; i < benchmarks.size(); ++i) {
          iterationRun(benchmarks.get(i), Options.Iteration.Mode.FUNCTION, dataPack.substring(FILE_PREFIX.length()), i == benchmarks.size() - 1);
        }
      }
    }

    for (final var format : mchConfig.formats()) {
      format.write(mchConfig, mcVersion, runResults);
    }
  }

  private List<String> collectBenchmarkFunctions(
    final String dataPack
  ) throws IOException {
    final var functions = new ArrayList<String>();
    final var root = Paths.get(levelName, "datapacks", dataPack, "data");
    final var visitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        final var relativePath = root.relativize(file);
        if (MCFUNCTION_MATCHER.matches(relativePath)) {
          try (final var reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null && (line = line.trim()).startsWith("#")) {
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

  private void setupRun(
    final boolean dry
  ) throws IOException, InterruptedException {
    final var options = new Options.Setup(dry).toString();
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

  private List<String> collectBenchmarkDataPacks() throws IOException {
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
            if (Boolean.TRUE.equals(packMetadata.pack.mch)) {
              return Stream.of(dataPacksRoot.relativize(packMetadataPath.getParent()).toString());
            } else {
              return Stream.of();
            }
          } catch (IOException e) {
            return Stream.of();
          }
        })
        .toList();
    }
  }

  private void iterationRun(
    final String benchmark,
    final Options.Iteration.Mode mode,
    final String group,
    final boolean lastInGroup
  ) throws IOException, InterruptedException {
    final var scores = new ArrayList<Double>();
    for (var fork = 0; fork < mchConfig.forks(); ++fork) {
      try (final var server = new ServerSocket(0)) {
        final var thread = runIterationThread(server, scores);

        final var lastIterationInGroup = lastInGroup && fork == mchConfig.forks() - 1;
        final var port = server.getLocalPort();
        final var progress = 100.0f * done++ / total;
        final var options = new Options.Iteration(
          mchConfig.autoStart(),
          lastIterationInGroup,
          mchConfig.warmupIterations(),
          mchConfig.measurementIterations(),
          mchConfig.time(),
          mchConfig.forks(),
          fork,
          port,
          progress,
          mode,
          benchmark
        ).toString();

        Spy.create(
          Paths.get(mchConfig.mc()),
          MchCommands.class,
          options,
          mchConfig.mcArgs().toArray(new String[0]),
          mchConfig.jvmArgs().toArray(new String[0])
        ).inheritIO().start().waitFor();

        thread.join();
      }
    }
    runResults.add(new RunResult(group, benchmark, mode, scores));
  }

  @Keep
  private record PackMetadata(
    @Keep PackMetadataSection pack
  ) {
  }

  @Keep
  private record PackMetadataSection(
    @Keep int packFormat,
    @Keep Boolean mch
  ) {
  }
}
