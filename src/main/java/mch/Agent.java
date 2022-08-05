package mch;

import joptsimple.OptionParser;
import mch.transformers.CommandInjector;
import mch.transformers.ModNameTransformer;
import mch.transformers.Transformer;

import java.lang.instrument.Instrumentation;
import java.util.Map;

public class Agent {
    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("Starting mch.Agent");

        var parser = new OptionParser();
        var modeSpec = parser.accepts("mode").withRequiredArg().ofType(MchCommands.Mode.class);
        var benchmarksSpec = parser.accepts("benchmarks").withOptionalArg().ofType(String.class).withValuesSeparatedBy(',');
        var indexSpec = parser.accepts("index").withOptionalArg().ofType(Integer.class);

        var options = parser.parse(args.split(";"));
        var mode = options.valueOf(modeSpec);
        var benchmarks = options.valuesOf(benchmarksSpec);
        var index = options.valueOf(indexSpec);
        var benchmark = benchmarks.get(index);

        instrumentation.addTransformer(new Transformer(Map.of(
                "net/minecraft/server/MinecraftServer", ModNameTransformer::new,
                "com/mojang/brigadier/CommandDispatcher", v -> new CommandInjector(v, benchmark)
        )));
    }
}
