package dev.mcenv.mch.fork;

import dev.mcenv.mch.Keep;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

@Keep
public final class Fork {
  @Keep
  public static void main(
    final String[] args
  ) throws Throwable {
    System.out.println("Starting dev.mcenv.mch.fork.Fork");

    final var server = System.getProperty("mch.server");
    final var classLoader = new URLClassLoader(new URL[]{Paths.get(server).toUri().toURL()});
    final var mainClass = Class.forName("net.minecraft.bundler.Main", true, classLoader);
    final var mainHandle = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(Void.TYPE, String[].class)).asFixedArity();
    mainHandle.invoke((Object) args);
  }
}
