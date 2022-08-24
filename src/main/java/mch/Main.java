package mch;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static mch.Util.bytesToDouble;
import static mch.Util.quote;

public final class Main {
    public static void main(
            final String[] args
    ) throws InterruptedException {
        System.out.println("Starting mch.Main");

        try {
            final var benchmarks = Files.readAllLines(Paths.get("benchmarks"));

            final var results = new ArrayList<Result>();
            for (final var benchmark : benchmarks) {
                forkProcess(results, benchmark, args);
            }
            for (final var result : results) {
                System.out.println(result.benchmark() + " " + result.stat().mean() + " ± " + result.stat().error() + " ns/op");
            }
        } catch (final IOException e) {
            System.err.println("not found: 'benchmarks'");
            System.exit(1);
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
                        results.add(new Result(benchmark, new Stat(scores)));
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
}
