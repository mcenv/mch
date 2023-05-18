package dev.mcenv.mch;

import dev.mcenv.mch.Keep;

import java.util.Collection;

@Keep
public record Results(
  @Keep String mchVersion,
  @Keep int forks,
  @Keep String jvm,
  @Keep Collection<String> jvmArgs,
  @Keep String jdkVersion,
  @Keep String vmName,
  @Keep String vmVersion,
  @Keep String mc,
  @Keep Collection<String> mcArgs,
  @Keep int warmupIterations,
  @Keep String warmupTime,
  @Keep int measurementIterations,
  @Keep String measurementTime,
  @Keep Collection<Result> results
) {
  @Keep
  public record Result(
    @Keep String benchmark,
    @Keep String mode,
    @Keep int count,
    @Keep double score,
    @Keep double error,
    @Keep String unit
  ) {
  }
}
