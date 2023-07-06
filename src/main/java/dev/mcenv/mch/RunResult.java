package dev.mcenv.mch;

import java.util.Collection;

record RunResult(
  String group,
  String benchmark,
  Options.Iteration.Mode mode,
  Collection<Double> scores
) {
}
