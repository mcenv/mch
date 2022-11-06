package mch;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static mch.Util.bytesToDouble;
import static mch.Util.quote;

public final class Main {
    public static void main(
            final String[] args
    ) throws InterruptedException, IOException {
        System.out.println("Starting mch.Main");

        installDatapack();

        final var config = new GlobalConfig(getOrCreateProperties());

        final var results = new LinkedHashMap<String, Collection<Double>>();
        for (final var benchmark : config.benchmarks()) {
            forkProcess(results, config, benchmark, args);
        }

        dumpResults(results, config);
    }

    private static Properties getOrCreateProperties() throws IOException {
        final var properties = new Properties();
        final var path = Paths.get("mch.properties");
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try (final var input = Files.newInputStream(path)) {
                properties.load(input);
            }
        }
        properties.putIfAbsent(GlobalConfig.WARMUP_ITERATIONS_KEY, String.valueOf(GlobalConfig.WARMUP_ITERATIONS_DEFAULT));
        properties.putIfAbsent(GlobalConfig.MEASUREMENT_ITERATIONS_KEY, String.valueOf(GlobalConfig.MEASUREMENT_ITERATIONS_DEFAULT));
        properties.putIfAbsent(GlobalConfig.TIME_KEY, String.valueOf(GlobalConfig.TIME_DEFAULT));
        properties.putIfAbsent(GlobalConfig.FORKS_KEY, String.valueOf(GlobalConfig.FORKS_DEFAULT));
        properties.putIfAbsent(GlobalConfig.BENCHMARKS, GlobalConfig.BENCHMARKS_DEFAULT);
        try (final var output = Files.newOutputStream(path)) {
            properties.store(output, null);
        }
        return properties;
    }

    private static void installDatapack() throws IOException {
        final var serverProperties = Paths.get("server.properties");
        var levelName = "world";
        if (Files.exists(serverProperties)) {
            final var properties = new Properties();
            try (final var input = Files.newInputStream(serverProperties)) {
                properties.load(input);
            }
            levelName = properties.getProperty("level-name");
        }

        final var datapack = Paths.get(levelName, "datapacks", "mch.zip");
        if (Files.notExists(datapack)) {
            Files.createDirectories(datapack.getParent());
            try (final var output = new BufferedOutputStream(Files.newOutputStream(datapack))) {
                try (final var input = Main.class.getResourceAsStream("/mch.zip")) {
                    Objects.requireNonNull(input).transferTo(output);
                }
            }
        }
    }

    private static void forkProcess(
            final Map<String, Collection<Double>> results,
            final GlobalConfig config,
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
                final var command = getCommand(config, fork, port, benchmark, args);
                final var builder = new ProcessBuilder(command);
                final var process = builder.start();
                process.getInputStream().transferTo(System.out);
                process.waitFor();

                thread.join();
            }
        }
    }

    private static List<String> getCommand(
            final GlobalConfig config,
            final int fork,
            final int port,
            final String benchmark,
            final String[] args
    ) {
        try {
            final var java = ProcessHandle.current().info().command().orElseThrow();
            final var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
            final var options = quote(new LocalConfig(
                    config.warmupIterations(),
                    config.measurementIterations(),
                    config.time(),
                    config.forks(),
                    fork,
                    port,
                    benchmark
            ).toString());
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
            final GlobalConfig config
    ) throws IOException {
        try (final var output = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.json")))) {
            output.write('[');

            {
                var i = 0;
                for (final var entry : results.entrySet()) {
                    final var benchmark = entry.getKey();
                    final var values = entry.getValue().stream().mapToDouble(x -> x).toArray();
                    if (i++ != 0) {
                        output.write(',');
                    }
                    output.write(
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
                }
            }

            output.write('\n');
            output.write(']');
            output.write('\n');
        }
    }
}
