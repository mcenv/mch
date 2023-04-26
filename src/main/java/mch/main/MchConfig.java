package mch.main;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import mch.Keep;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static mch.Util.parseTimeUnit;

@Keep
public record MchConfig(
  @Keep int warmupIterations,
  @Keep int measurementIterations,
  @Keep int time,
  @Keep int forks,
  @Keep TimeUnit timeUnit,
  @Keep String mc,
  @Keep Collection<String> jvmArgs,
  @Keep Collection<String> mcArgs,
  @Keep Collection<String> parsingBenchmarks,
  @Keep Collection<String> executeBenchmarks
) {
  public static final int WARMUP_ITERATIONS_DEFAULT = 5;
  public static final int MEASUREMENT_ITERATIONS_DEFAULT = 5;
  public static final int TIME_DEFAULT = 10;
  public static final int FORKS_DEFAULT = 5;
  public static final TimeUnit TIME_UNIT_DEFAULT = TimeUnit.SECONDS;
  public static final String MC_DEFAULT = "server.jar";
  public static final String MC_ARGS_DEFAULT = "nogui";

  public static final class Deserializer implements JsonDeserializer<MchConfig> {
    @Override
    public MchConfig deserialize(
      final JsonElement json,
      final Type typeOfT,
      final JsonDeserializationContext context
    ) throws JsonParseException {
      final var object = json.getAsJsonObject();

      final var warmupIterations = object.get("warmup_iterations") != null ? object.get("warmup_iterations").getAsInt() : WARMUP_ITERATIONS_DEFAULT;
      final var measurementIterations = object.get("measurement_iterations") != null ? object.get("measurement_iterations").getAsInt() : MEASUREMENT_ITERATIONS_DEFAULT;
      final var time = object.get("time") != null ? object.get("time").getAsInt() : TIME_DEFAULT;
      final var forks = object.get("forks") != null ? object.get("forks").getAsInt() : FORKS_DEFAULT;
      final var timeUnit = object.get("time_unit") != null ? parseTimeUnit(object.get("time_unit").getAsString()) : TIME_UNIT_DEFAULT;
      final var mc = object.get("mc") != null ? object.get("mc").getAsString() : MC_DEFAULT;
      final var jvmArgs = object.get("jvm_args") != null ? object.get("jvm_args").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.<String>of();
      final var mcArgs = object.get("mc_args") != null ? object.get("mc_args").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.of(MC_ARGS_DEFAULT);
      final var parsingBenchmarks = object.get("parsing_benchmarks") != null ? object.get("parsing_benchmarks").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.<String>of();
      final var executeBenchmark = object.get("execute_benchmarks") != null ? object.get("execute_benchmarks").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : List.<String>of();

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
