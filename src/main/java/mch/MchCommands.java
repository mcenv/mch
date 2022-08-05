package mch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.Collections;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class MchCommands {
    private static long startTime;
    private static long iterationCount;
    private static long operationCount;
    private static ParseResults<Object> run;

    public static void register(final CommandDispatcher<Object> dispatcher, final String target) {
        dispatcher.register(
                literal("mch")
                        .then(
                                literal("start")
                                        .executes(c -> start(dispatcher, target, c.getSource()))
                        )
                        .then(
                                literal("run")
                                        .executes(c -> run(dispatcher))
                        )
                        .then(
                                literal("stop_or")
                                        .fork(dispatcher.getRoot(), c -> stopOr(c.getSource()))
                        )
        );
    }

    private static int start(final CommandDispatcher<Object> dispatcher, final String target, final Object source) {
        startTime = System.nanoTime();
        ++iterationCount;
        operationCount = 0;
        run = dispatcher.parse("function mch:" + target /* TODO */, source);
        return 0;
    }

    private static int run(final CommandDispatcher<Object> dispatcher) throws CommandSyntaxException {
        dispatcher.execute(run);
        ++operationCount;
        return 0;
    }

    private static Collection<Object> stopOr(final Object source) {
        final var current = System.nanoTime();
        if (current - startTime < 1000000000L) {
            return Collections.singletonList(source);
        } else {
            final var stopTime = System.nanoTime();
            final var result = (double) (stopTime - startTime) / (double) operationCount + " ns/op";
            final var iterationName = iterationCount <= 5 ? "Warmup iteration" : "Iteration";
            System.out.println(iterationName + ": " + result);
            return Collections.emptyList();
        }
    }

    public enum Mode {
        INIT,
        RUN
    }
}
