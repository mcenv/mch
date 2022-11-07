import proguard.gradle.ProGuardTask

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("com.guardsquare:proguard-gradle:7.2.2")
  }
}

plugins {
  java
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

version = "0.3.0"

repositories {
  mavenCentral()
  maven("https://libraries.minecraft.net")
}

dependencies {
  implementation("org.ow2.asm:asm:9.4")
  implementation("org.apache.commons:commons-math3:3.6.1")
  compileOnly("com.mojang:brigadier:1.0.18")
}

tasks.withType(JavaCompile::class) {
  options.encoding = "UTF-8"
  options.release.set(17)
}

tasks.jar {
  manifest {
    attributes(
      "Premain-Class" to "mch.Agent",
      "Main-Class" to "mch.Main",
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
  from(layout.projectDirectory.dir("datapack"))
  archiveFileName.set("mch.zip")
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

tasks.getByName<ProcessResources>("processResources") {
  dependsOn(tasks.getByName("zipDatapack"))
  from(layout.buildDirectory.file("distributions/mch.zip"))
}

tasks.register<ProGuardTask>("optimizeJar") {
  dependsOn(tasks.getByName("shadowJar"))

  injars(layout.buildDirectory.file("libs/mch-shadow.jar"))
  outjars(layout.buildDirectory.file("libs/mch.jar"))

  System
    .getProperty("java.home")
    .let { javaHome ->
      libraryjars("$javaHome/jmods/java.base.jmod")
      libraryjars("$javaHome/jmods/java.desktop.jmod")
      libraryjars("$javaHome/jmods/java.instrument.jmod")
    }
  libraryjars(configurations.compileClasspath.get().asFileTree.find { it.endsWith("brigadier-1.0.18.jar") })

  keep("@interface mch.Keep")
  keep("@mch.Keep class *")
  keepclassmembers("class * { @mch.Keep *; }")

  optimizationpasses(5)
  repackageclasses("mch")
  dontwarn("java.lang.invoke.MethodHandle")
}
