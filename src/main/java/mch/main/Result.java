package mch.main;

import mch.Keep;
import mch.Options;

import java.util.Collection;

public record Result(
  Collection<Double> scores,
  Options.Iteration.Mode mode
) {

  @Keep
  public record Entry(
    @Keep String benchmark,
    @Keep String mode,
    @Keep int count,
    @Keep double score,
    @Keep double error,
    @Keep String unit
  ) {
  }
}
