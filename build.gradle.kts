import proguard.gradle.ProGuardTask

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("com.guardsquare:proguard-gradle:7.3.2")
  }
}

plugins {
  java
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

version = "0.9.1"
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
}

tasks.withType(JavaCompile::class) {
  options.encoding = "UTF-8"
  options.release.set(17)
}

tasks.jar {
  manifest {
    attributes(
      "Premain-Class" to "dev.mcenv.mch.agent.Agent",
      "Main-Class" to "dev.mcenv.mch.main.Main",
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

@Suppress("UnstableApiUsage")
tasks.withType<ProcessResources> {
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

  optimizationpasses(optimizationPasses)
  repackageclasses("mch")
  dontwarn("java.lang.invoke.MethodHandle")
}

tasks.register<ProGuardTask>("optimizeJar") {
  optimizeJar(10)
}

tasks.register<ProGuardTask>("developmentOptimizeJar") {
  optimizeJar(0)
}
