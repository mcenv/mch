package mch;

import com.mojang.brigadier.CommandDispatcher;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class MchCommands {
    public static void register(CommandDispatcher<Object> dispatcher, String name) {
        dispatcher.register(
                literal("mch")
                        .executes(c -> {
                            var source = c.getSource();
                            dispatcher.execute("say " + name, source);

                            Runtime.getRuntime().halt(0);
                            return 0;
                        })
        );
    }
}
