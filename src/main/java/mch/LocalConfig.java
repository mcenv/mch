package mch;

public record LocalConfig(
        int warmupIterations,
        int measurementIterations,
        int time,
        String benchmark,
        int port
) {
    public static LocalConfig parse(
            final String string
    ) {
        final var args = string.split(",");
        return new LocalConfig(
                Integer.parseInt(args[0]),
                Integer.parseInt(args[1]),
                Integer.parseInt(args[2]),
                args[3],
                Integer.parseInt(args[4])
        );
    }
}
