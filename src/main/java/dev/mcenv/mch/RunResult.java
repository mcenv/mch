package dev.mcenv.mch;

import java.util.Collection;

record RunResult(
  Collection<Double> scores,
  Options.Iteration.Mode mode
) {
}
