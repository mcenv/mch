plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
    implementation("org.ow2.asm:asm:9.3")
    implementation("com.mojang:brigadier:1.0.18")
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
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.test {
    useJUnitPlatform()
}
