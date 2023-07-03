package dev.mcenv.mch;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static dev.mcenv.mch.Util.*;

sealed interface Format permits Format.Json, Format.Md {
  static Format parse(final String format) {
    return switch (format) {
      case "json" -> Json.INSTANCE;
      case "md" -> Md.INSTANCE;
      default -> throw new IllegalStateException("Unexpected format: " + format);
    };
  }

  void write(
    final MchConfig mchConfig,
    final String mcVersion,
    final Map<String, RunResult> runResults
  ) throws IOException;

  final class Json implements Format {
    public static final Format INSTANCE = new Json();

    private Json() {
    }

    @Override
    public void write(
      final MchConfig mchConfig,
      final String mcVersion,
      final Map<String, RunResult> runResults
    ) throws IOException {
      final var unit = String.format("%s/op", abbreviate(mchConfig.timeUnit()));
      try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.json")))) {
        final String mchVersion;
        try (final var version = Main.class.getClassLoader().getResourceAsStream("version")) {
          mchVersion = new String(Objects.requireNonNull(version).readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        final var forks = mchConfig.forks();
        final var jvm = getCurrentJvm();
        final var jvmArgs = mchConfig.jvmArgs();
        final var jdkVersion = System.getProperty("java.version");
        final var vmName = System.getProperty("java.vm.name");
        final var vmVersion = System.getProperty("java.vm.version");
        final var mc = mchConfig.mc();
        final var mcArgs = mchConfig.mcArgs();
        final var warmupIterations = mchConfig.warmupIterations();
        final var warmupTime = String.format("%d %s", mchConfig.time(), abbreviate(TimeUnit.SECONDS));
        final var measurementIterations = mchConfig.measurementIterations();
        final var measurementTime = String.format("%d %s", mchConfig.time(), abbreviate(TimeUnit.SECONDS));
        final var entries = runResults
          .entrySet()
          .stream()
          .map(entry -> {
            final var benchmark = entry.getKey();
            final var runResult = entry.getValue();
            final var values = runResult.scores().stream().mapToDouble(x -> x).toArray();
            try {
              return new Results.Result(
                benchmark,
                runResult.mode().toString(),
                mchConfig.measurementIterations() * mchConfig.forks(),
                convert(Statistics.mean(values), TimeUnit.NANOSECONDS, mchConfig.timeUnit()),
                convert(Statistics.error(values), TimeUnit.NANOSECONDS, mchConfig.timeUnit()),
                unit
              );
            } catch (NotStrictlyPositiveException e) {
              throw new RuntimeException(e);
            }
          })
          .toList();

        final var gson = new GsonBuilder().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        out.write(gson
          .toJson(new Results(
            mchVersion,
            forks,
            jvm,
            jvmArgs,
            jdkVersion,
            vmName,
            vmVersion,
            mc,
            mcArgs,
            mcVersion,
            warmupIterations,
            warmupTime,
            measurementIterations,
            measurementTime,
            entries
          ))
          .getBytes(StandardCharsets.UTF_8)
        );
      }
    }
  }

  final class Md implements Format {
    public static final Format INSTANCE = new Md();

    private Md() {
    }

    @Override
    public void write(
      final MchConfig mchConfig,
      final String mcVersion,
      final Map<String, RunResult> runResults
    ) throws IOException {
      try (final var out = new BufferedOutputStream(Files.newOutputStream(Paths.get("mch-results.md")))) {
        // TODO
      }
    }
  }
}