package mch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static mch.Util.doubleToBytes;

public final class MchCommands {
    private static long startTime;
    private static long iterationCount;
    private static long operationCount;
    private static ParseResults<Object> run;
    private static Socket socket;

    public static void register(final CommandDispatcher<Object> dispatcher, final String benchmark) {
        try {
            socket = new Socket("localhost", 25585);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dispatcher.register(
                literal("mch")
                        .then(
                                literal("start")
                                        .executes(c -> start(dispatcher, benchmark, c.getSource()))
                        )
                        .then(
                                literal("run")
                                        .executes(c -> run(dispatcher))
                        )
                        .then(
                                literal("loop")
                                        .fork(dispatcher.getRoot(), c -> loop(c.getSource()))
                        )
                        .then(
                                literal("stop")
                                        .executes(c -> stop())
                        )
        );
    }

    private static int start(final CommandDispatcher<Object> dispatcher, final String benchmark, final Object source) {
        startTime = System.nanoTime();
        ++iterationCount;
        operationCount = 0;
        run = dispatcher.parse("function " + benchmark, source);
        return 0;
    }

    private static int run(final CommandDispatcher<Object> dispatcher) throws CommandSyntaxException {
        dispatcher.execute(run);
        ++operationCount;
        return 0;
    }

    private static Collection<Object> loop(final Object source) {
        final var current = System.nanoTime();
        if (current - startTime < 1000000000L) {
            return Collections.singletonList(source);
        } else {
            final var stopTime = System.nanoTime();
            final var result = (double) (stopTime - startTime) / (double) operationCount;
            final var iterationType = iterationCount <= 5 ? "Warmup" : "Measurement";

            System.out.println(iterationType + " iteration: " + result + " ns/op");
            try {
                socket.getOutputStream().write(doubleToBytes(result));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return Collections.emptyList();
        }
    }

    private static int stop() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
