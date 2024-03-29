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

@Keep
record MchConfig(
  @Keep boolean autoStart,
  @Keep int warmupIterations,
  @Keep int measurementIterations,
  @Keep int time,
  @Keep int forks,
  @Keep TimeUnit timeUnit,
  @Keep String mc,
  @Keep String output,
  @Keep Collection<Format> formats,
  @Keep Collection<String> jvmArgs,
  @Keep Collection<String> mcArgs,
  @Keep Collection<String> parsingBenchmarks,
  @Keep Collection<String> executeBenchmarks
) {
  public static final String AUTO_START = "auto_start";
  public static final String WARMUP_ITERATIONS = "warmup_iterations";
  public static final String MEASUREMENT_ITERATIONS = "measurement_iterations";
  public static final String TIME = "time";
  public static final String FORKS = "forks";
  public static final String TIME_UNIT = "time_unit";
  public static final String MC = "mc";
  public static final String OUTPUT = "output";
  public static final String FORMATS = "formats";
  public static final String JVM_ARGS = "jvm_args";
  public static final String MC_ARGS = "mc_args";
  public static final String PARSING_BENCHMARKS = "parsing_benchmarks";
  public static final String EXECUTE_BENCHMARKS = "execute_benchmarks";

  public static final boolean AUTO_START_DEFAULT = true;
  public static final int WARMUP_ITERATIONS_DEFAULT = 5;
  public static final int MEASUREMENT_ITERATIONS_DEFAULT = 5;
  public static final int TIME_DEFAULT = 10;
  public static final int FORKS_DEFAULT = 5;
  public static final TimeUnit TIME_UNIT_DEFAULT = TimeUnit.SECONDS;
  public static final String MC_DEFAULT = "server.jar";
  public static final String OUTPUT_DEFAULT = "mch-results";
  public static final Collection<Format> FORMATS_DEFAULT = List.of();
  public static final String MC_ARGS_DEFAULT = "nogui";

  public static class Builder {
    private final String[] args;
    private boolean autoStart = AUTO_START_DEFAULT;
    private int warmupIterations = WARMUP_ITERATIONS_DEFAULT;
    private int measurementIterations = MEASUREMENT_ITERATIONS_DEFAULT;
    private int time = TIME_DEFAULT;
    private int forks = FORKS_DEFAULT;
    private TimeUnit timeUnit = TIME_UNIT_DEFAULT;
    private String mc = MC_DEFAULT;
    private String output = OUTPUT_DEFAULT;
    private Collection<Format> formats = FORMATS_DEFAULT;
    private Collection<String> jvmArgs = List.of();
    private Collection<String> mcArgs = List.of(MC_ARGS_DEFAULT);
    private Collection<String> parsingBenchmarks = List.of();
    private Collection<String> executeBenchmarks = List.of();

    public Builder(final String[] args) {
      this.args = args;
    }

    public MchConfig build() {
      final var parser = new OptionParser();
      final var autoStartSpec = parser.accepts(AUTO_START).withOptionalArg().ofType(Boolean.class);
      final var warmupIterationsSpec = parser.accepts(WARMUP_ITERATIONS).withOptionalArg().ofType(Integer.class);
      final var measurementIterationsSpec = parser.accepts(MEASUREMENT_ITERATIONS).withOptionalArg().ofType(Integer.class);
      final var timeSpec = parser.accepts(TIME).withOptionalArg().ofType(Integer.class);
      final var forksSpec = parser.accepts(FORKS).withOptionalArg().ofType(Integer.class);
      final var timeUnitSpec = parser.accepts(TIME_UNIT).withOptionalArg().ofType(String.class);
      final var mcSpec = parser.accepts(MC).withOptionalArg().ofType(String.class);
      final var outputSpec = parser.accepts(OUTPUT).withOptionalArg().ofType(String.class);
      final var formatSpec = parser.accepts(FORMATS).withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var jvmArgsSpec = parser.accepts(JVM_ARGS).withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var mcArgsSpec = parser.accepts(MC_ARGS).withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var parsingBenchmarksSpec = parser.accepts(PARSING_BENCHMARKS).withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var executeBenchmarksSpec = parser.accepts(EXECUTE_BENCHMARKS).withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
      final var options = parser.parse(args);

      if (options.has(autoStartSpec)) {
        autoStart = options.valueOf(autoStartSpec);
      }
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
      if (options.has(formatSpec)) {
        formats = options.valuesOf(formatSpec).stream().map(Format::parse).toList();
      }
      if (options.has(jvmArgsSpec)) {
        jvmArgs = options.valuesOf(jvmArgsSpec);
      }
      if (options.has(mcArgsSpec)) {
        mcArgs = options.valuesOf(mcArgsSpec);
      }
      if (options.has(outputSpec)) {
        output = options.valueOf(outputSpec);
      }
      if (options.has(parsingBenchmarksSpec)) {
        parsingBenchmarks = options.valuesOf(parsingBenchmarksSpec);
      }
      if (options.has(executeBenchmarksSpec)) {
        executeBenchmarks = options.valuesOf(executeBenchmarksSpec);
      }

      return new MchConfig(
        autoStart,
        warmupIterations,
        measurementIterations,
        time,
        forks,
        timeUnit,
        mc,
        output,
        formats,
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

      if (object.get(AUTO_START) != null) {
        super.autoStart = object.get(AUTO_START).getAsBoolean();
      }
      if (object.get(WARMUP_ITERATIONS) != null) {
        super.warmupIterations = object.get(WARMUP_ITERATIONS).getAsInt();
      }
      if (object.get(MEASUREMENT_ITERATIONS) != null) {
        super.measurementIterations = object.get(MEASUREMENT_ITERATIONS).getAsInt();
      }
      if (object.get(TIME) != null) {
        super.time = object.get(TIME).getAsInt();
      }
      if (object.get(FORKS) != null) {
        super.forks = object.get(FORKS).getAsInt();
      }
      if (object.get(TIME_UNIT) != null) {
        super.timeUnit = parseTimeUnit(object.get(TIME_UNIT).getAsString());
      }
      if (object.get(MC) != null) {
        super.mc = object.get(MC).getAsString();
      }
      if (object.get(OUTPUT) != null) {
        super.output = object.get(OUTPUT).getAsString();
      }
      if (object.get(FORMATS) != null) {
        super.formats = object.get(FORMATS).getAsJsonArray().asList().stream().map(JsonElement::getAsString).map(Format::parse).toList();
      }
      if (object.get(JVM_ARGS) != null) {
        super.jvmArgs = object.get(JVM_ARGS).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }
      if (object.get(MC_ARGS) != null) {
        super.mcArgs = object.get(MC_ARGS).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }
      if (object.get(PARSING_BENCHMARKS) != null) {
        super.parsingBenchmarks = object.get(PARSING_BENCHMARKS).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }
      if (object.get(EXECUTE_BENCHMARKS) != null) {
        super.executeBenchmarks = object.get(EXECUTE_BENCHMARKS).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
      }

      return build();
    }
  }
}
