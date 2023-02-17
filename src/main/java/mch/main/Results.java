package mch.main;

import mch.Keep;

import java.util.Collection;

@Keep
public record Results(
  @Keep String mchVersion,
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
