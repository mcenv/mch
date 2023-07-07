package dev.mcenv.mch;

import java.util.Collection;

@Keep
record Results(
  @Keep String mchVersion,
  @Keep int forks,
  @Keep String jvm,
  @Keep Collection<String> jvmArgs,
  @Keep String jdkVersion,
  @Keep String vmName,
  @Keep String vmVersion,
  @Keep String mc,
  @Keep Collection<String> mcArgs,
  @Keep String mcVersion,
  @Keep int warmupIterations,
  @Keep String warmupTime,
  @Keep int measurementIterations,
  @Keep String measurementTime,
  @Keep Collection<Result> results
) {
  @Keep
  public record Result(
    @Keep String group,
    @Keep String benchmark,
    @Keep String mode,
    @Keep int count,
    @Keep double score,
    @Keep double error,
    @Keep String unit
  ) {
  }
}
