package mch;

import mch.transformers.CommandInjector;
import mch.transformers.ModNameTransformer;
import mch.transformers.Transformer;

import java.lang.instrument.Instrumentation;
import java.util.Map;

public final class Agent {
    public static void premain(
            final String args,
            final Instrumentation instrumentation
    ) {
        System.out.println("Starting mch.Agent");

        final var arguments = args.split(",");
        final var benchmark = arguments[0];
        final var port = Integer.parseInt(arguments[1]);

        instrumentation.addTransformer(new Transformer(Map.of(
                "net/minecraft/server/MinecraftServer", ModNameTransformer::new,
                "com/mojang/brigadier/CommandDispatcher", v -> new CommandInjector(v, benchmark, port)
        )));
    }
}
