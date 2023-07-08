import proguard.gradle.ProGuardTask
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
  java
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.mcenv"
version = "0.14.0"
val brigadierVersion = "1.1.8"

repositories {
  mavenCentral()
  maven("https://libraries.minecraft.net")
  maven {
    url = uri("https://maven.pkg.github.com/mcenv/spy")
    credentials {
      username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
      password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
    }
  }
}

dependencies {
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("dev.mcenv:spy:0.5.0")
  implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
  implementation("org.ow2.asm:asm:9.5")
  implementation("org.apache.commons:commons-math3:3.6.1")
  implementation("org.commonmark:commonmark:0.20.0")
  implementation("org.commonmark:commonmark-ext-gfm-tables:0.20.0")
}

tasks.withType(JavaCompile::class) {
  options.encoding = "UTF-8"
  options.release.set(17)
}

tasks.jar {
  manifest {
    attributes(
      "Premain-Class" to "dev.mcenv.spy.Agent",
      "Main-Class" to "dev.mcenv.mch.Main",
    )
  }
}

tasks.shadowJar {
  archiveClassifier.set("shadow")
  archiveVersion.set("")
}

tasks.test {
  useJUnitPlatform()
}

tasks.register<Zip>("zipDatapack") {
  from(layout.projectDirectory.dir("mch"))
  archiveFileName.set("mch.zip")
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

tasks.register("computeHash") {
  dependsOn(tasks.getByName("zipDatapack"))
  doLast {
    val digest = MessageDigest.getInstance("SHA-256")
    DigestInputStream(layout.buildDirectory.file("distributions/mch.zip").get().asFile.inputStream().buffered(), digest).use { input ->
      input.readBytes()
    }
    layout.buildDirectory.file("distributions/hash").get().asFile.outputStream().buffered().use { output ->
      output.write(digest.digest())
    }
  }
}

tasks.withType<ProcessResources> {
  dependsOn(tasks.getByName("computeHash"))
  from(
    layout.buildDirectory.file("distributions/mch.zip"),
    layout.buildDirectory.file("distributions/hash"),
  )
  filesMatching("version") {
    expand("version" to version)
  }
}

fun ProGuardTask.optimizeJar(optimizationPasses: Int) {
  dependsOn(tasks.getByName("shadowJar"))

  injars(layout.buildDirectory.file("libs/mch-shadow.jar"))
  outjars(layout.buildDirectory.file("libs/mch.jar"))

  System
    .getProperty("java.home")
    .let { javaHome ->
      libraryjars("$javaHome/jmods/java.base.jmod")
      libraryjars("$javaHome/jmods/java.desktop.jmod")
      libraryjars("$javaHome/jmods/java.instrument.jmod")
      libraryjars("$javaHome/jmods/java.sql.jmod")
    }
  libraryjars(configurations.compileClasspath.get().asFileTree.find { it.endsWith("brigadier-$brigadierVersion.jar") })

  keep("@interface dev.mcenv.mch.Keep")
  keep("@dev.mcenv.mch.Keep class *")
  keepclassmembers("class * { @dev.mcenv.mch.Keep *; }")
  keep("class dev.mcenv.spy.Agent { public static void premain(java.lang.String, java.lang.instrument.Instrumentation); }")
  keep("class dev.mcenv.spy.Fork { public static void main(java.lang.String[]); }")

  optimizationpasses(optimizationPasses)
  repackageclasses("mch")
  dontwarn("java.lang.invoke.MethodHandle")
}

tasks.register<ProGuardTask>("optimizeJar") {
  optimizeJar(20)
}

tasks.register<ProGuardTask>("developmentOptimizeJar") {
  optimizeJar(0)
}
