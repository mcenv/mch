package mch.main;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import joptsimple.OptionParser;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static mch.Util.parseTimeUnit;

public record MchConfig(
  int warmupIterations,
  int measurementIterations,
  int time,
  int forks,
  TimeUnit timeUnit,
  String mc,
  Collection<String> jvmArgs,
  Collection<String> mcArgs,
  Collection<String> parsingBenchmarks,
  Collection<String> executeBenchmarks
) {
  public static final int WARMUP_ITERATIONS_DEFAULT = 5;
  public static final int MEASUREMENT_ITERATIONS_DEFAULT = 5;
  public static final int TIME_DEFAULT = 10;
  public static final int FORKS_DEFAULT = 5;
  public static final TimeUnit TIME_UNIT_DEFAULT = TimeUnit.SECONDS;
  public static final String MC_DEFAULT = "server.jar";
  public static final String MC_ARGS_DEFAULT = "nogui";

  public static final class Deserializer implements JsonDeserializer<MchConfig> {
    private final String[] args;

    public Deserializer(
      final String[] args
    ) {
      this.args = args;
    }

    @Override
    public MchConfig deserialize(
      final JsonElement json,
      final Type typeOfT,
      final JsonDeserializationContext context
    ) throws JsonParseException {
      final var parser = new OptionParser();
      final var warmupIterationsSpec = parser.accepts("warmup-iterations").withOptionalArg().ofType(Integer.class);
      final var measurementIterationsSpec = parser.accepts("measurement-iterations").withOptionalArg().ofType(Integer.class);
      final var timeSpec = parser.accepts("time").withOptionalArg().ofType(Integer.class);
      final var forksSpec = parser.accepts("forks").withOptionalArg().ofType(Integer.class);
      final var timeUnitSpec = parser.accepts("time-unit").withOptionalArg().ofType(String.class);
      final var mcSpec = parser.accepts("mc").withOptionalArg().ofType(String.class);
      final var jvmArgsSpec = parser.accepts("jvm-args").withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var mcArgsSpec = parser.accepts("mc-args").withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var parsingBenchmarksSpec = parser.accepts("parsing-benchmarks").withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var executeBenchmarkSpec = parser.accepts("execute-benchmarks").withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var options = parser.parse(args);

      final var object = json.getAsJsonObject();
      final var warmupIterations = options.has(warmupIterationsSpec) ? options.valueOf(warmupIterationsSpec) : object.get("warmup_iterations") != null ? object.get("warmup_iterations").getAsInt() : WARMUP_ITERATIONS_DEFAULT;
      final var measurementIterations = options.has(measurementIterationsSpec) ? options.valueOf(measurementIterationsSpec) : object.get("measurement_iterations") != null ? object.get("measurement_iterations").getAsInt() : MEASUREMENT_ITERATIONS_DEFAULT;
      final var time = options.has(timeSpec) ? options.valueOf(timeSpec) : object.get("time") != null ? object.get("time").getAsInt() : TIME_DEFAULT;
      final var forks = options.has(forksSpec) ? options.valueOf(forksSpec) : object.get("forks") != null ? object.get("forks").getAsInt() : FORKS_DEFAULT;
      final var timeUnit = options.has(timeUnitSpec) ? parseTimeUnit(options.valueOf(timeUnitSpec)) : object.get("time_unit") != null ? parseTimeUnit(object.get("time_unit").getAsString()) : TIME_UNIT_DEFAULT;
      final var mc = options.has(mcSpec) ? options.valueOf(mcSpec) : object.get("mc") != null ? object.get("mc").getAsString() : MC_DEFAULT;
      final var jvmArgs = options.has(jvmArgsSpec) ? options.valuesOf(jvmArgsSpec) : object.get("jvm_args") != null ? object.get("jvm_args").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.<String>of();
      final var mcArgs = options.has(mcArgsSpec) ? options.valuesOf(mcArgsSpec) : object.get("mc_args") != null ? object.get("mc_args").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.of(MC_ARGS_DEFAULT);
      final var parsingBenchmarks = options.has(parsingBenchmarksSpec) ? options.valuesOf(parsingBenchmarksSpec) : object.get("parsing_benchmarks") != null ? object.get("parsing_benchmarks").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.<String>of();
      final var executeBenchmark = options.has(executeBenchmarkSpec) ? options.valuesOf(executeBenchmarkSpec) : object.get("execute_benchmarks") != null ? object.get("execute_benchmarks").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.<String>of();

      return new MchConfig(
        warmupIterations,
        measurementIterations,
        time,
        forks,
        timeUnit,
        mc,
        jvmArgs,
        mcArgs,
        parsingBenchmarks,
        executeBenchmark
      );
    }
  }
}
