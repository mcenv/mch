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
@Keep
public final class MchCommands {
  private static long startTime;
  private static int iterationCount;
  private static int operationCount;
  private static ParseResults<Object> run;
  private static ParseResults<Object> loop;

  private MchCommands() {
  }

  @Keep
  public static void register(
    final CommandDispatcher<Object> dispatcher,
    final Options options
  ) throws IOException {
    if (options instanceof Options.Dry) {
      registerDry(dispatcher);
    } else if (options instanceof Options.Iteration iteration) {
      registerIteration(dispatcher, iteration);
    }
  }

  private static void registerDry(
    final CommandDispatcher<Object> dispatcher
  ) {
    dispatcher.register(
      literal("mch:start").executes(c -> {
        System.out.println("Dry run");
        return 0;
      })
    );

    dispatcher.register(
      literal("mch:loop").executes(c -> 0)
    );
  }

  private static void registerIteration(
    final CommandDispatcher<Object> dispatcher,
    final Options.Iteration options
  ) throws IOException {
    final var socket = new Socket((String) null, options.port());
    final int warmupCount = options.warmupIterations();
    final int measurementCount = options.warmupIterations() + options.measurementIterations();
    final long time = TimeUnit.SECONDS.toNanos(options.time());

    dispatcher.register(
      literal("mch:start").executes(c -> {
        System.out.println(options.benchmark() + " " + (options.fork() + 1) + "/" + options.forks());

        final var source = c.getSource();
        run = dispatcher.parse("function " + options.benchmark(), source);
        loop = dispatcher.parse("function mch:loop", source);

        startTime = System.nanoTime();

        try {
          dispatcher.execute(run);
        } catch (final CommandSyntaxException e1) {
          System.out.println(e1.getMessage());
          try {
            socket.close();
            dispatcher.execute("function mch:post", source);
          } catch (IOException e2) {
            throw new RuntimeException(e2);
          }
        }
        dispatcher.execute(loop);
        ++operationCount;

        return 0;
      })
    );

    dispatcher.register(
      literal("mch:loop").executes(c -> {
        try {
          final var stopTime = System.nanoTime();
          if (stopTime - startTime >= time) {
            final var result = (double) (stopTime - startTime) / (double) operationCount;

            if (iterationCount < warmupCount) {
              System.out.println("Warmup iteration: " + result + " ns/op");
            } else if (iterationCount < measurementCount) {
              System.out.println("Measurement iteration: " + result + " ns/op");
              socket.getOutputStream().write(doubleToBytes(result));
            } else {
              socket.close();
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
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      })
    );
  }
}
