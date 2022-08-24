package mch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public final class Fork {
    public static void main(
            final String[] args
    ) throws Throwable {
        System.out.println("Starting mch.Fork");

        final var classLoader = new URLClassLoader(new URL[]{Paths.get("server.jar").toUri().toURL()});
        final var mainClass = Class.forName("net.minecraft.bundler.Main", true, classLoader);
        final var mainHandle = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(Void.TYPE, String[].class)).asFixedArity();
        mainHandle.invoke((Object) args);
    }
}
