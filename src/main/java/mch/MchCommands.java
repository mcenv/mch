package mch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.net.Socket;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static mch.Util.doubleToBytes;

public final class MchCommands {
    private static long startTime;
    private static long iterationCount;
    private static long operationCount;
    private static ParseResults<Object> run;
    private static ParseResults<Object> loop;
    private static Socket socket;

    public static void register(
            final CommandDispatcher<Object> dispatcher,
            final String benchmark,
            final int port
    ) {
        try {
            socket = new Socket((String) null, port);
        } catch (final IOException e) {
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
                                        .executes(c -> loop(dispatcher))
                        )
                        .then(
                                literal("stop")
                                        .executes(c -> stop())
                        )
        );
    }

    private static int start(
            final CommandDispatcher<Object> dispatcher,
            final String benchmark,
            final Object source
    ) throws CommandSyntaxException {
        startTime = System.nanoTime();
        ++iterationCount;
        operationCount = 0;
        run = dispatcher.parse("function " + benchmark, source);
        if (loop == null) {
            loop = dispatcher.parse("function mch:loop", source);
        }
        dispatcher.execute(loop);
        return 0;
    }

    private static int run(
            final CommandDispatcher<Object> dispatcher
    ) throws CommandSyntaxException {
        dispatcher.execute(run);
        ++operationCount;
        return 0;
    }

    private static int loop(
            final CommandDispatcher<Object> dispatcher
    ) throws CommandSyntaxException {
        final var current = System.nanoTime();
        if (current - startTime < 1000000000L) {
            dispatcher.execute(loop);
        } else {
            final var stopTime = System.nanoTime();
            final var result = (double) (stopTime - startTime) / (double) operationCount;

            if (iterationCount <= 5) {
                System.out.println("Warmup iteration: " + result + " ns/op");
            } else {
                System.out.println("Measurement iteration: " + result + " ns/op");
                try {
                    socket.getOutputStream().write(doubleToBytes(result));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return 0;
    }

    private static int stop() {
        try {
            socket.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
