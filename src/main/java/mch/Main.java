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

        final List<String> benchmarks;
        try {
            benchmarks = Files.readAllLines(Paths.get("benchmarks"));
        } catch (final IOException e) {
            System.err.println("not found: 'benchmarks'");
            System.exit(1);
            return;
        }

        final var results = new ArrayList<Result>();
        for (final var benchmark : benchmarks) {
            forkProcess(results, benchmark, args);
        }

        dumpResults(results);
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
            final Collection<Result> results,
            final String benchmark,
            final String[] args
    ) throws IOException, InterruptedException {
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
                        results.add(new Result(benchmark, scores));
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            thread.start();

            final var port = server.getLocalPort();
            final var command = getCommand(benchmark, port, args);
            final var builder = new ProcessBuilder(command);
            final var process = builder.start();
            process.getInputStream().transferTo(System.out);
            process.waitFor();

            thread.join();
        }
    }

    private static List<String> getCommand(
            final String benchmark,
            final int port,
            final String[] args
    ) {
        try {
            final var java = ProcessHandle.current().info().command().orElseThrow();
            final var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
            final var options = quote(benchmark + ',' + port);
            final var result = new ArrayList<String>();
            Collections.addAll(result, java, "-javaagent:" + jar + "=" + options, "-cp", jar, "mch.Fork", "nogui");
            Collections.addAll(result, args);
            return result;
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void dumpResults(
            final List<Result> results
    ) throws IOException {
        try (final var output = new BufferedOutputStream(Files.newOutputStream(Paths.get("results.json")))) {
            output.write('[');
            for (var i = 0; i < results.size(); ++i) {
                if (i != 0) {
                    output.write(',');
                }
                final var result = results.get(i);
                output.write(
                        String.format("""
                                        \n  { "benchmark": "%s", "count": %d, "score": %f, "error": %f, "unit": "%s" }""",
                                result.benchmark(),
                                5,
                                result.mean(),
                                result.error(),
                                "ns/op"
                        ).getBytes(StandardCharsets.UTF_8)
                );
            }
            output.write('\n');
            output.write(']');
            output.write('\n');
        }
    }
}
