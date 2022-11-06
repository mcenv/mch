package mch;

import org.apache.commons.math3.distribution.TDistribution;

public final class Statistics {
    public static double mean(
            final double[] values
    ) {
        var result = 0.0;
        for (final var value : values) {
            result += value;
        }
        return result / values.length;
    }

    private static double variance(
            final double[] values
    ) {
        var result = 0.0;
        final var mean = mean(values);
        for (final var value : values) {
            result += Math.pow(value - mean, 2.0);
        }
        return result / (values.length - 1);
    }

    public static double error(
            final double[] values
    ) {
        return new TDistribution(values.length - 1)
                .inverseCumulativeProbability(0.9995)
                * Math.sqrt(variance(values))
                / Math.sqrt(values.length);
    }
}
