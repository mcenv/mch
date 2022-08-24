package mch;

import org.apache.commons.math3.distribution.TDistribution;

import java.util.Collection;

public final class Result {
    private final String benchmark;
    private final Collection<Double> values;

    public Result(
            final String benchmark,
            final Collection<Double> values
    ) {
        this.benchmark = benchmark;
        this.values = values;
    }

    public String benchmark() {
        return benchmark;
    }

    public double mean() {
        var result = 0.0;
        for (final var value : values) {
            result += value;
        }
        return result / values.size();
    }

    private double variance() {
        var result = 0.0;
        final var mean = mean();
        for (final var value : values) {
            result += Math.pow(value - mean, 2.0);
        }
        return result / (values.size() - 1);
    }

    public double error() {
        return new TDistribution(values.size() - 1)
                .inverseCumulativeProbability(0.9995)
                * Math.sqrt(variance())
                / Math.sqrt(values.size());
    }
}
