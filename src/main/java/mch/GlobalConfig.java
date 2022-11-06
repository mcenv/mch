package mch;

import java.util.Properties;

public record GlobalConfig(
        int warmupIterations,
        int measurementIterations,
        int time,
        int forks,
        String[] benchmarks
) {
    public static final String WARMUP_ITERATIONS_KEY = "warmup-iterations";
    public static final int WARMUP_ITERATIONS_DEFAULT = 5;

    public static final String MEASUREMENT_ITERATIONS_KEY = "measurement-iterations";
    public static final int MEASUREMENT_ITERATIONS_DEFAULT = 5;

    public static final String TIME_KEY = "time";
    public static final int TIME_DEFAULT = 10;

    public static final String FORKS_KEY = "forks";
    public static final int FORKS_DEFAULT = 5;

    public static final String BENCHMARKS = "benchmarks";
    public static final String BENCHMARKS_DEFAULT = ",";

    public GlobalConfig(
            final Properties properties
    ) {
        this(
                Integer.parseInt(properties.getProperty(WARMUP_ITERATIONS_KEY, String.valueOf(WARMUP_ITERATIONS_DEFAULT))),
                Integer.parseInt(properties.getProperty(MEASUREMENT_ITERATIONS_KEY, String.valueOf(MEASUREMENT_ITERATIONS_DEFAULT))),
                Integer.parseInt(properties.getProperty(TIME_KEY, String.valueOf(TIME_DEFAULT))),
                Integer.parseInt(properties.getProperty(FORKS_KEY, String.valueOf(FORKS_DEFAULT))),
                properties.getProperty(BENCHMARKS, BENCHMARKS_DEFAULT).split(",")
        );
    }
}
