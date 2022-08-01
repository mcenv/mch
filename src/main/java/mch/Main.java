package mch;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Throwable {
        System.out.println("Starting mch.Main");

        var classLoader = new URLClassLoader(new URL[]{Paths.get("server.jar").toUri().toURL()});
        var mainClass = Class.forName("net.minecraft.bundler.Main", true, classLoader);
        var mainHandle = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(Void.TYPE, String[].class)).asFixedArity();
        mainHandle.invoke(args);
    }
}
