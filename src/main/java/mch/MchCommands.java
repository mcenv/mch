package mch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static mch.Util.doubleToBytes;

@SuppressWarnings("unused")
public final class MchCommands {
    private static long startTime;
    private static int iterationCount;
    private static int operationCount;
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
                literal("mch:start").executes(c -> {
                    System.out.println(benchmark);

                    final var source = c.getSource();
                    run = dispatcher.parse("function " + benchmark, source);
                    loop = dispatcher.parse("function mch:loop", source);

                    startTime = System.nanoTime();

                    dispatcher.execute(run);
                    dispatcher.execute(loop);
                    ++operationCount;

                    return 0;
                })
        );

        dispatcher.register(
                literal("mch:loop").executes(c -> {
                    final var stopTime = System.nanoTime();
                    if (stopTime - startTime >= 10000000000L) {
                        final var result = (double) (stopTime - startTime) / (double) operationCount;

                        if (iterationCount < 5) {
                            System.out.println("Warmup iteration: " + result + " ns/op");
                        } else if (iterationCount < 10) {
                            System.out.println("Measurement iteration: " + result + " ns/op");
                            try {
                                socket.getOutputStream().write(doubleToBytes(result));
                            } catch (final IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return 0;
                        }

                        ++iterationCount;
                        operationCount = 0;
                        startTime = System.nanoTime();
                    }

                    dispatcher.execute(run);
                    dispatcher.execute(loop);
                    ++operationCount;

                    return 0;
                })
        );
    }
}
