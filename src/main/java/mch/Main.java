package mch;

import joptsimple.OptionException;
import joptsimple.OptionParser;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static mch.Util.bytesToDouble;
import static mch.Util.quote;

public final class Main {
    public static void main(
            final String[] args
    ) throws IOException, InterruptedException {
        System.out.println("Starting mch.Main");

        final var parser = new OptionParser();
        final var benchmarksSpec = parser.acceptsAll(List.of("b", "benchmarks")).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
        final var minecraftSpec = parser.acceptsAll(List.of("m", "minecraft")).withOptionalArg().ofType(String.class).defaultsTo("nogui");

        try {
            final var options = parser.parse(args);
            final var benchmarks = options.valuesOf(benchmarksSpec);
            final var minecraft = options.valueOf(minecraftSpec);

            final var results = new ArrayList<Result>();
            for (final var benchmark : benchmarks) {
                forkProcess(results, benchmark, minecraft);
            }
            for (final var result : results) {
                System.out.println(result.benchmark() + " " + result.stat().mean() + " ± " + result.stat().error() + " ns/op");
            }
        } catch (final OptionException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void forkProcess(
            final Collection<Result> results,
            final String benchmark,
            final String minecraft
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
                        results.add(new Result(benchmark, new Stat(scores)));
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            thread.start();

            final var port = server.getLocalPort();
            final var command = getCommand(benchmark, port, minecraft);
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
            final String minecraft
    ) {
        try {
            final var java = ProcessHandle.current().info().command().orElseThrow();
            final var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
            final var options = quote(benchmark + ',' + port);
            return List.of(java, "-javaagent:" + jar + "=" + options, "-cp", jar, "mch.Fork", minecraft);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
