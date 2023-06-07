package dev.mcenv.mch;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import joptsimple.OptionParser;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.mcenv.mch.Util.parseTimeUnit;

record MchConfig(
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

  public static class Builder {
    private final String[] args;
    private int warmupIterations = WARMUP_ITERATIONS_DEFAULT;
    private int measurementIterations = MEASUREMENT_ITERATIONS_DEFAULT;
    private int time = TIME_DEFAULT;
    private int forks = FORKS_DEFAULT;
    private TimeUnit timeUnit = TIME_UNIT_DEFAULT;
    private String mc = MC_DEFAULT;
    private Collection<String> jvmArgs = List.of();
    private Collection<String> mcArgs = List.of(MC_ARGS_DEFAULT);
    private Collection<String> parsingBenchmarks = List.of();
    private Collection<String> executeBenchmarks = List.of();

    public Builder(final String[] args) {
      this.args = args;
    }

    public MchConfig build() {
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

      if (options.has(warmupIterationsSpec)) {
        warmupIterations = options.valueOf(warmupIterationsSpec);
        if (warmupIterations < 0) {
          throw new IllegalStateException("Warmup iterations must be greater than or equal to 0");
        }
      }
      if (options.has(measurementIterationsSpec)) {
        measurementIterations = options.valueOf(measurementIterationsSpec);
        if (measurementIterations < 2) {
          throw new IllegalStateException("Measurement iterations must be greater than 1");
        }
      }
      if (options.has(timeSpec)) {
        time = options.valueOf(timeSpec);
        if (time < 1) {
          throw new IllegalStateException("Time must be greater than 0");
        }
      }
      if (options.has(forksSpec)) {
        forks = options.valueOf(forksSpec);
        if (forks < 1) {
          throw new IllegalStateException("Forks must be greater than 0");
        }
      }
      if (options.has(timeUnitSpec)) {
        timeUnit = parseTimeUnit(options.valueOf(timeUnitSpec));
      }
      if (options.has(mcSpec)) {
        mc = options.valueOf(mcSpec);
      }
      if (options.has(jvmArgsSpec)) {
        jvmArgs = options.valuesOf(jvmArgsSpec);
      }
      if (options.has(mcArgsSpec)) {
        mcArgs = options.valuesOf(mcArgsSpec);
      }
      if (options.has(parsingBenchmarksSpec)) {
        parsingBenchmarks = options.valuesOf(parsingBenchmarksSpec);
      }
      if (options.has(executeBenchmarkSpec)) {
        executeBenchmarks = options.valuesOf(executeBenchmarkSpec);
      }

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
        executeBenchmarks
      );
    }
  }

  public static final class Deserializer extends Builder implements JsonDeserializer<MchConfig> {
    public Deserializer(final String[] args) {
      super(args);
    }

    @Override
    public MchConfig deserialize(
      final JsonElement json,
      final Type typeOfT,
      final JsonDeserializationContext context
    ) throws JsonParseException {
      final var object = json.getAsJsonObject();

      if (object.get("warmup_iterations") != null) {
        super.warmupIterations = object.get("warmup_iterations").getAsInt();
      }
      if (object.get("measurement_iterations") != null) {
        super.measurementIterations = object.get("measurement_iterations").getAsInt();
      }
      if (object.get("time") != null) {
        super.time = object.get("time").getAsInt();
      }
      if (object.get("forks") != null) {
        super.forks = object.get("forks").getAsInt();
      }
      if (object.get("time_unit") != null) {
        super.timeUnit = parseTimeUnit(object.get("time_unit").getAsString());
      }
      if (object.get("mc") != null) {
        super.mc = object.get("mc").getAsString();
      }
      if (object.get("jvm_args") != null) {
        super.jvmArgs = object.get("jvm_args").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }
      if (object.get("mc_args") != null) {
        super.mcArgs = object.get("mc_args").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }
      if (object.get("parsing_benchmarks") != null) {
        super.parsingBenchmarks = object.get("parsing_benchmarks").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }
      if (object.get("execute_benchmarks") != null) {
        super.executeBenchmarks = object.get("execute_benchmarks").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }

      return build();
    }
  }
}
