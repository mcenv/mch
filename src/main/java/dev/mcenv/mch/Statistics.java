package dev.mcenv.mch;

import org.apache.commons.math3.distribution.TDistribution;

import java.util.Collection;

final class Statistics {
  public static double mean(
    final Collection<Double> values
  ) {
    var result = 0.0;
    for (final var value : values) {
      result += value;
    }
    return result / values.size();
  }

  private static double variance(
    final Collection<Double> values
  ) {
    var result = 0.0;
    final var mean = mean(values);
    for (final var value : values) {
      result += Math.pow(value - mean, 2.0);
    }
    return result / (values.size() - 1);
  }

  public static double error(
    final Collection<Double> values
  ) {
    return new TDistribution(values.size() - 1)
      .inverseCumulativeProbability(0.9995)
      * Math.sqrt(variance(values))
      / Math.sqrt(values.size());
  }
}
