package mch.agent;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mch.Keep;
import mch.Options;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static mch.Util.doubleToBytes;

@SuppressWarnings("unused")
@Keep
public final class MchCommands {
  private static final String START = "mch:start";
  private static final String LOOP = "mch:loop";
  private static final String POST = "mch:post";
  private static final String NOOP = "mch:noop";

  private static long startTime;
  private static int iterationCount;
  private static int operationCount;
  private static double[] results;
  private static ParseResults<Object> run;
  private static ParseResults<Object> loop;
  private static ParseResults<Object> post;
  private static ParseResults<Object> setupIteration;
  private static ParseResults<Object> teardownIteration;

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
      switch (iteration.mode()) {
        case PARSING -> registerParsingIteration(dispatcher, iteration);
        case EXECUTE -> registerExecuteIteration(dispatcher, iteration);
      }
    }
  }

  private static void registerDry(
    final CommandDispatcher<Object> dispatcher
  ) {
    dispatcher.register(
      literal(START).executes(c -> dispatcher.execute("function #mch:setup", c.getSource()))
    );

    registerConst(dispatcher, LOOP);
    registerConst(dispatcher, POST);
    registerConst(dispatcher, NOOP);
  }

  private static void registerParsingIteration(
    final CommandDispatcher<Object> dispatcher,
    final Options.Iteration options
  ) throws IOException {
    final var socket = new Socket((String) null, options.port());
    final var warmupCount = options.warmupIterations();
    final var measurementCount = options.warmupIterations() + options.measurementIterations();
    final var time = TimeUnit.SECONDS.toNanos(options.time());
    results = new double[options.measurementIterations()];

    final var commands = prepareCommands(dispatcher, options.benchmark());

    dispatcher.register(
      literal(START).executes(c -> {
        System.out.println(options.mode() + " " + options.benchmark() + " " + (options.fork() + 1) + "/" + options.forks());

        if (commands == null) {
          try {
            System.out.println("No file " + options.benchmark() + " was found");
            socket.close();
            dispatcher.execute(post);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return 0;
        }

        var startTime = System.nanoTime();
        final var source = c.getSource();

        while (true) {
          for (final var command : commands) {
            dispatcher.parse(command, source);
          }
          final var stopTime = System.nanoTime();
          ++operationCount;

          if (stopTime - startTime >= time) {
            if (iterationCount < measurementCount) {
              final var result = (double) (stopTime - startTime) / (double) operationCount;
              if (iterationCount < warmupCount) {
                System.out.println("Warmup iteration: " + result + " ns/op");
              } else {
                System.out.println("Measurement iteration: " + result + " ns/op");
                results[iterationCount - warmupCount] = result;
              }
            } else {
              return 0;
            }

            ++iterationCount;
            operationCount = 0;
            startTime = System.nanoTime();
          }
        }
      })
    );

    registerConst(dispatcher, LOOP);

    dispatcher.register(
      literal(POST).executes(c -> {
        try {
          for (final var result : results) {
            socket.getOutputStream().write(doubleToBytes(result));
          }
          socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return 0;
      })
    );

    registerConst(dispatcher, NOOP);
  }

  private static String[] prepareCommands(
    final CommandDispatcher<Object> dispatcher,
    final String benchmark
  ) throws IOException {
    final var path = Paths.get(benchmark);
    if (Files.exists(path) && Files.isRegularFile(path)) {
      try (final var in = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
        return in
          .lines()
          .flatMap(line -> {
            final var normalized = line.trim();
            if (normalized.isEmpty() || normalized.startsWith("#")) {
              return Stream.empty();
            } else {
              return Stream.of(normalized);
            }
          })
          .toList()
          .toArray(new String[]{});
      }
    } else {
      return null;
    }
  }

  private static void registerExecuteIteration(
    final CommandDispatcher<Object> dispatcher,
    final Options.Iteration options
  ) throws IOException {
    final var socket = new Socket((String) null, options.port());
    final var warmupCount = options.warmupIterations();
    final var measurementCount = options.warmupIterations() + options.measurementIterations();
    final var time = TimeUnit.SECONDS.toNanos(options.time());
    results = new double[options.measurementIterations()];

    dispatcher.register(
      literal(START).executes(c -> {
        System.out.println(options.mode() + " " + options.benchmark() + " " + (options.fork() + 1) + "/" + options.forks());

        parseFunctions(dispatcher, c.getSource(), options.benchmark());
        dispatcher.execute(setupIteration);

        startTime = System.nanoTime();

        try {
          dispatcher.execute(run);
        } catch (final CommandSyntaxException e1) {
          System.out.println(e1.getMessage());
          try {
            socket.close();
            dispatcher.execute(post);
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
      literal(LOOP).executes(c -> {
        final var stopTime = System.nanoTime();
        if (stopTime - startTime >= time) {
          if (iterationCount < measurementCount) {
            dispatcher.execute(teardownIteration);

            final var result = (double) (stopTime - startTime) / (double) operationCount;
            if (iterationCount < warmupCount) {
              System.out.println("Warmup iteration: " + result + " ns/op");
            } else {
              System.out.println("Measurement iteration: " + result + " ns/op");
              results[iterationCount - warmupCount] = result;
            }

            if (iterationCount < measurementCount - 1) {
              dispatcher.execute(setupIteration);
            }
          } else {
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

    dispatcher.register(
      literal(POST).executes(c -> {
        try {
          for (final var result : results) {
            socket.getOutputStream().write(doubleToBytes(result));
          }
          socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return 0;
      })
    );

    registerConst(dispatcher, NOOP);
  }

  private static void parseFunctions(
    final CommandDispatcher<Object> dispatcher,
    final Object source,
    final String benchmark
  ) {
    run = dispatcher.parse("function " + benchmark, source);
    loop = dispatcher.parse("function mch:loop", source);
    post = dispatcher.parse("function mch:post", source);
    setupIteration = dispatcher.parse("function #mch:setup.iteration", source);
    teardownIteration = dispatcher.parse("function #mch:teardown.iteration", source);
  }

  private static void registerConst(
    final CommandDispatcher<Object> dispatcher,
    final String name
  ) {
    dispatcher.register(literal(name).executes(c -> 0));
  }
}
