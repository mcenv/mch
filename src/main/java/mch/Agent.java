package mch;

import mch.transformers.CommandInjector;
import mch.transformers.ModNameTransformer;
import mch.transformers.Transformer;

import java.lang.instrument.Instrumentation;
import java.util.Map;

public class Agent {
    public static void premain(final String args, final Instrumentation instrumentation) {
        System.out.println("Starting mch.Agent");

        instrumentation.addTransformer(new Transformer(Map.of(
                "net/minecraft/server/MinecraftServer", ModNameTransformer::new,
                "com/mojang/brigadier/CommandDispatcher", v -> new CommandInjector(v, args)
        )));
    }
}
