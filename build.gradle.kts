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
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("com.mojang:brigadier:1.0.500")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "mch.Main",
            "Launcher-Agent-Class" to "mch.Agent",
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
