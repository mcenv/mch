plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

version = "0.2.0"

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
