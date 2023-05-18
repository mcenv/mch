package dev.mcenv.mch.main;

import dev.mcenv.mch.Options;

import java.util.Collection;

public record RunResult(
  Collection<Double> scores,
  Options.Iteration.Mode mode
) {
}
