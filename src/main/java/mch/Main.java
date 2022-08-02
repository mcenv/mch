package mch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting mch.Main");

        var builder = new ProcessBuilder(getCommand(args));
        var process = builder.start();
        process.waitFor();
    }

    private static List<String> getCommand(String[] args) {
        try {
            var java = ProcessHandle.current().info().command().orElseThrow();
            String jar = '"' + Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString() + '"';

            var command = new ArrayList<String>();
            command.addAll(List.of(java, "-javaagent:" + jar, "-cp", jar, "mch.Fork"));
            command.addAll(Arrays.asList(args));
            return command;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
