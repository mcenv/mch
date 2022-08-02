package mch;

import joptsimple.OptionParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting mch.Main");

        var parser = new OptionParser();
        var benchmarksSpec = parser.acceptsAll(List.of("b", "benchmarks")).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
        var minecraftSpec = parser.acceptsAll(List.of("m", "minecraft")).withOptionalArg().ofType(String.class).defaultsTo("nogui");

        var options = parser.parse(args);
        var benchmarks = options.valuesOf(benchmarksSpec);
        var minecraft = options.valueOf(minecraftSpec);

        for (var benchmark : benchmarks) {
            forkProcess(benchmark, minecraft);
        }
    }

    private static int forkProcess(String benchmark, String minecraft) throws IOException, InterruptedException {
        var builder = new ProcessBuilder(getCommand(benchmark, minecraft));
        var process = builder.start();
        process.getInputStream().transferTo(System.out);
        return process.waitFor();
    }

    private static List<String> getCommand(String benchmark, String minecraft) {
        try {
            var java = ProcessHandle.current().info().command().orElseThrow();
            var jar = quote(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString());
            return List.of(java, "-javaagent:" + jar + "=" + quote(benchmark), "-cp", jar, "mch.Fork", minecraft);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String quote(String string) {
        return '"' + string + '"';
    }
}
