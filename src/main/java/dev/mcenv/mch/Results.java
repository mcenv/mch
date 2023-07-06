package dev.mcenv.mch;

import java.util.Collection;

record Results(
  String mchVersion,
  int forks,
  String jvm,
  Collection<String> jvmArgs,
  String jdkVersion,
  String vmName,
  String vmVersion,
  String mc,
  Collection<String> mcArgs,
  String mcVersion,
  int warmupIterations,
  String warmupTime,
  int measurementIterations,
  String measurementTime,
  Collection<Result> results
) {
  public record Result(
    String group,
    String benchmark,
    String mode,
    int count,
    double score,
    double error,
    String unit
  ) {
  }
}
