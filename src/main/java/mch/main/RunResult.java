package mch.main;

import mch.Options;

import java.util.Collection;

public record RunResult(
  Collection<Double> scores,
  Options.Iteration.Mode mode
) {
}
