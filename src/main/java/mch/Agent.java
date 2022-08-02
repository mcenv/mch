package mch;

import java.lang.instrument.Instrumentation;

public class Agent {
    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("Starting mch.Agent");

        instrumentation.addTransformer(new Transformer());
    }
}
