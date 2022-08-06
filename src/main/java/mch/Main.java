package mch;

import joptsimple.OptionParser;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static mch.Util.bytesToDouble;
import static mch.Util.quote;

public final class Main {
    public static void main(final String[] args) throws IOException, InterruptedException {
        System.out.println("Starting mch.Main");

        final var parser = new OptionParser();
        final var benchmarksSpec = parser.acceptsAll(List.of("b", "benchmarks")).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
        final var minecraftSpec = parser.acceptsAll(List.of("m", "minecraft")).withOptionalArg().ofType(String.class).defaultsTo("nogui");

        final var options = parser.parse(args);
        final var benchmarks = options.valuesOf(benchmarksSpec);
        final var minecraft = options.valueOf(minecraftSpec);

        for (final var benchmark : benchmarks) {
            forkProcess(benchmark, minecraft);
        }
    }

    private static void forkProcess(final String benchmark, final String minecraft) throws IOException, InterruptedException {
        try (final var server = new ServerSocket(0)) {
            final var thread = new Thread(() -> {
                try {
                    final var client = server.accept();
                    try (final var in = client.getInputStream()) {
                        final var buffer = new byte[8];
                        while (in.readNBytes(buffer, 0, 8) == 8) {
                            System.out.println(bytesToDouble(buffer));
                        }
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
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

    private static List<String> getCommand(final String benchmark, final int port, final String minecraft) {
        try {
            final var java = ProcessHandle.current().info().command().orElseThrow();
            final var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
            final var options = quote(benchmark + ',' + port);
            return List.of(java, "-javaagent:" + jar + "=" + options, "-cp", jar, "mch.Fork", minecraft);
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
