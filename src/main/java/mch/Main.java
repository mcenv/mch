package mch;

import joptsimple.OptionParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

public final class Main {
    public static void main(final String[] args) throws IOException, InterruptedException {
        System.out.println("Starting mch.Main");

        final var parser = new OptionParser();
        final var benchmarksSpec = parser.acceptsAll(List.of("b", "benchmarks")).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
        final var minecraftSpec = parser.acceptsAll(List.of("m", "minecraft")).withOptionalArg().ofType(String.class).defaultsTo("nogui");

        final var options = parser.parse(args);
        final var benchmarks = options.valuesOf(benchmarksSpec);
        final var minecraft = options.valueOf(minecraftSpec);

        for (var benchmark : benchmarks) {
            forkProcess(benchmark, minecraft);
        }
    }

    private static void forkProcess(final String benchmark, final String minecraft) throws IOException, InterruptedException {
        final var command = getCommand(benchmark, minecraft);
        final var builder = new ProcessBuilder(command);
        final var process = builder.start();
        process.getInputStream().transferTo(System.out);
        process.waitFor();
    }

    private static List<String> getCommand(final String benchmark, final String minecraft) {
        try {
            final var java = ProcessHandle.current().info().command().orElseThrow();
            final var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
            final var options = quote(benchmark);
            return List.of(java, "-javaagent:" + jar + "=" + options, "-cp", jar, "mch.Fork", minecraft);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String quote(final String string) {
        return '"' + string + '"';
    }
}
