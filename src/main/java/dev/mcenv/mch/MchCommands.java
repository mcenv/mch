package dev.mcenv.mch;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mcenv.spy.Commands;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

@Keep
@SuppressWarnings("unused")
public final class MchCommands implements Commands {
  private static final String LIMIT = "mch:limit";
  private static final String SETUP = "mch:setup";
  private static final String START = "mch:start";
  private static final String CHECK = "mch:check";
  private static final String LOOP = "mch:loop";
  private static final String POST = "mch:post";
  private static final String NOOP = "mch:noop";

  private Socket socket;
  private int limited = 0;
  private boolean maxCommandChainLengthExceeded = true;
  private long startTime;
  private int iterationCount;
  private int operationCount;
  private double[] scores;
  private ParseResults<Object> run;
  private ParseResults<Object> loop;
  private ParseResults<Object> post;
  private ParseResults<Object> setupIteration;
  private ParseResults<Object> teardownIteration;

  @Keep
  @Override
  public void register(
    final CommandDispatcher<Object> dispatcher,
    final String args
  ) {
    registerConst(dispatcher, NOOP);

    final var options = Options.parse(args);
    if (options instanceof Options.Dry) {
      registerDry(dispatcher);
    } else if (options instanceof Options.Iteration iterationOptions) {
      try {
        socket = new Socket((String) null, iterationOptions.port());
        switch (iterationOptions.mode()) {
          case PARSING -> registerParsingIteration(dispatcher, iterationOptions);
          case EXECUTE -> registerExecuteIteration(dispatcher, iterationOptions);
          case FUNCTION -> registerFunctionIteration(dispatcher, iterationOptions);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void registerDry(
    final CommandDispatcher<Object> dispatcher
  ) {
    dispatcher.register(literal(LIMIT).executes(c -> {
      final var source = c.getSource();
      if (limited++ == 0) {
        dispatcher.execute("gamerule maxCommandChainLength 2147483647", source);
      } else {
        dispatcher.execute("gamerule maxCommandChainLength 0", source);
        dispatcher.execute("stop", source);
      }
      return 0;
    }));
    dispatcher.register(literal(SETUP).executes(c -> dispatcher.execute("function #mch:setup", c.getSource())));
    registerConst(dispatcher, START);
    registerConst(dispatcher, CHECK);
    registerConst(dispatcher, LOOP);
    registerConst(dispatcher, POST);
  }

  private void registerParsingIteration(
    final CommandDispatcher<Object> dispatcher,
    final Options.Iteration options
  ) throws IOException {
    final var warmupCount = options.warmupIterations();
    final var measurementCount = options.warmupIterations() + options.measurementIterations();
    final var time = TimeUnit.SECONDS.toNanos(options.time());
    scores = new double[options.measurementIterations()];

    final var command = options.benchmark();

    dispatcher.register(literal(LIMIT).executes(c -> dispatcher.execute("gamerule maxCommandChainLength 2147483647", c.getSource())));

    registerConst(dispatcher, SETUP);

    dispatcher.register(
      literal(START).executes(c -> {
        printIteration(options);

        var startTime = System.nanoTime();
        final var source = c.getSource();

        while (true) {
          dispatcher.parse(command, source);
          final var stopTime = System.nanoTime();
          ++operationCount;

          if (stopTime - startTime >= time) {
            if (iterationCount < measurementCount) {
              final var result = (double) (stopTime - startTime) / (double) operationCount;
              if (iterationCount < warmupCount) {
                System.out.println("Warmup iteration: " + result + " ns/op");
              } else {
                System.out.println("Measurement iteration: " + result + " ns/op");
                scores[iterationCount - warmupCount] = result;
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

    registerConst(dispatcher, CHECK);

    registerConst(dispatcher, LOOP);

    dispatcher.register(
      literal(POST).executes(c -> {
        try {
          try (final var out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(new Message.RunResult(scores));
          }
          socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return 0;
      })
    );
  }

  private void registerExecuteIteration(
    final CommandDispatcher<Object> dispatcher,
    final Options.Iteration options
  ) throws IOException {
    final var warmupCount = options.warmupIterations();
    final var measurementCount = options.warmupIterations() + options.measurementIterations();
    final var time = TimeUnit.SECONDS.toNanos(options.time());
    scores = new double[options.measurementIterations()];

    dispatcher.register(literal(LIMIT).executes(c -> dispatcher.execute("gamerule maxCommandChainLength 2147483647", c.getSource())));

    registerConst(dispatcher, SETUP);

    dispatcher.register(
      literal(START).executes(c -> {
        printIteration(options);

        var startTime = System.nanoTime();
        final var source = c.getSource();
        final var command = dispatcher.parse(options.benchmark(), source);

        while (true) {
          try {
            dispatcher.execute(command);
          } catch (final CommandSyntaxException ignored) {
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
                scores[iterationCount - warmupCount] = result;
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

    registerConst(dispatcher, CHECK);

    registerConst(dispatcher, LOOP);

    dispatcher.register(
      literal(POST).executes(c -> {
        try {
          try (final var out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(new Message.RunResult(scores));
          }
          socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return 0;
      })
    );
  }

  private void registerFunctionIteration(
    final CommandDispatcher<Object> dispatcher,
    final Options.Iteration options
  ) throws IOException {
    final var warmupCount = options.warmupIterations();
    final var measurementCount = options.warmupIterations() + options.measurementIterations();
    final var time = TimeUnit.SECONDS.toNanos(options.time());
    scores = new double[options.measurementIterations()];

    dispatcher.register(
      literal(LIMIT).executes(c -> {
        if (options.autoStart() || limited++ >= 2) {
          dispatcher.execute("gamerule maxCommandChainLength 2147483647", c.getSource());
        } else if (limited == 1) {
          dispatcher.execute("gamerule maxCommandChainLength 0", c.getSource());
          System.out.println("Execute `/function #load` to start the benchmark");
        }
        return 0;
      })
    );

    registerConst(dispatcher, SETUP);

    dispatcher.register(
      literal(START).executes(c -> {
        printIteration(options);

        final var source = c.getSource();
        run = dispatcher.parse("function " + options.benchmark(), source);
        loop = dispatcher.parse("function mch:loop", source);
        post = dispatcher.parse("function mch:post", source);
        setupIteration = dispatcher.parse("function #mch:setup.iteration", source);
        teardownIteration = dispatcher.parse("function #mch:teardown.iteration", source);

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
      literal(CHECK).executes(c -> {
        maxCommandChainLengthExceeded = false;
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
              scores[iterationCount - warmupCount] = result;
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
          if (options.done() + 1 == options.total()) {
            dispatcher.execute("function #mch:teardown", c.getSource());
          }

          try (final var out = new ObjectOutputStream(socket.getOutputStream())) {
            if (maxCommandChainLengthExceeded) {
              System.err.println("maxCommandChainLength exceeded!");
              out.writeObject(new Message.MaxCommandChainLengthExceeded());
            } else {
              out.writeObject(new Message.RunResult(scores));
            }
          }
          socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return 0;
      })
    );
  }

  private void registerConst(
    final CommandDispatcher<Object> dispatcher,
    final String name
  ) {
    dispatcher.register(literal(name).executes(c -> 0));
  }

  private void printIteration(
    final Options.Iteration options
  ) {
    final var progress = 100.0 * options.done() / (double) options.total();
    System.out.println(options.mode() + " " + options.benchmark() + " " + (options.fork() + 1) + "/" + options.forks() + " (" + String.format("%.2f", progress) + "%)");
  }
}
