package mch.main;

import mch.Options;

import java.util.Collection;

public record Result(
  Collection<Double> scores,
  Options.Iteration.Mode mode
) {
}
