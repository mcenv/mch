package mch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.net.Socket;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static mch.Util.doubleToBytes;

@SuppressWarnings("unused")
public final class MchCommands {
    private static String benchmark;
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
        MchCommands.benchmark = benchmark;
        try {
            socket = new Socket((String) null, port);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        dispatcher.register(
                literal("mch")
                        .then(
                                literal("start")
                                        .executes(c -> start(dispatcher, c.getSource()))
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
            final Object source
    ) throws CommandSyntaxException {
        ++iterationCount;
        operationCount = 0;
        run = dispatcher.parse("function " + benchmark, source);
        if (loop == null) {
            loop = dispatcher.parse("function mch:loop", source);
        }
        System.out.println(benchmark);
        startTime = System.nanoTime();
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
        final var stopTime = System.nanoTime();
        if (stopTime - startTime < 10000000000L) {
            dispatcher.execute(loop);
        } else {
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
