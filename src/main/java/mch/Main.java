package mch;

import joptsimple.OptionParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(final String[] args) throws IOException, InterruptedException {
        System.out.println("Starting mch.Main");

        final var parser = new OptionParser();
        final var benchmarksSpec = parser.acceptsAll(List.of("b", "benchmarks")).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
        final var minecraftSpec = parser.acceptsAll(List.of("m", "minecraft")).withOptionalArg().ofType(String.class).defaultsTo("nogui");

        final var options = parser.parse(args);
        final var benchmarks = options.valuesOf(benchmarksSpec);
        final var minecraft = options.valueOf(minecraftSpec);

        for (var index = 0; index < benchmarks.size(); ++index) {
            forkProcess(benchmarks, index, minecraft);
        }
    }

    private static int forkProcess(final List<String> benchmarks, final int index, final String minecraft) throws IOException, InterruptedException {
        final var command = getCommand(benchmarks, index, minecraft);
        final var builder = new ProcessBuilder(command);
        final var process = builder.start();
        process.getInputStream().transferTo(System.out);
        return process.waitFor();
    }

    private static List<String> getCommand(final List<String> benchmarks, final int index, final String minecraft) {
        try {
            final var java = ProcessHandle.current().info().command().orElseThrow();
            final var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
            final var options = quote("--mode;RUN;--benchmarks;" + String.join(",", benchmarks) + ";--index;" + index);
            return List.of(java, "-javaagent:" + jar + "=" + options, "-cp", jar, "mch.Fork", minecraft);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String quote(final String string) {
        return '"' + string + '"';
    }
}
