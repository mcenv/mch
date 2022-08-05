package mch;

import joptsimple.OptionParser;
import mch.transformers.CommandInjector;
import mch.transformers.ModNameTransformer;
import mch.transformers.Transformer;

import java.lang.instrument.Instrumentation;
import java.util.Map;

public class Agent {
    public static void premain(final String args, final Instrumentation instrumentation) {
        System.out.println("Starting mch.Agent");

        final var parser = new OptionParser();
        final var modeSpec = parser.accepts("mode").withRequiredArg().ofType(MchCommands.Mode.class);
        final var benchmarksSpec = parser.accepts("benchmarks").withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
        final var indexSpec = parser.accepts("index").withOptionalArg().ofType(Integer.class);

        final var options = parser.parse(args.split(";"));
        final var mode = options.valueOf(modeSpec);
        final var benchmarks = options.valuesOf(benchmarksSpec);
        final var index = options.valueOf(indexSpec);
        final var benchmark = benchmarks.get(index);

        instrumentation.addTransformer(new Transformer(Map.of(
                "net/minecraft/server/MinecraftServer", ModNameTransformer::new,
                "com/mojang/brigadier/CommandDispatcher", v -> new CommandInjector(v, benchmark)
        )));
    }
}
