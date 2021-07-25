plugins {
    java
    id("fabric-loom") version "0.8-SNAPSHOT"
    id("com.github.ben-manes.versions") version "0.39.0"
}

val minecraftVersion = "1.17.1"
val yarnVersion = "$minecraftVersion+build.29:v2"
val fabricLoaderVersion = "0.11.6"
val fabricApiVersion = "0.37.1+1.17"
val modmenuVersion = "2.0.2"
val multiconnectVersion = "1.3.36"

group = "com.mumfrey.worldeditcui"
version = "$minecraftVersion+01-SNAPSHOT"

repositories {
    // mirrors:
    // - https://maven.enginehub.org/repo/
    // - https://maven.terraformersmc.com/releases/
    // - https://maven.minecraftforge.net/
    maven(url = "https://repo.stellardrift.ca/repository/stable/") {
        name = "stellardriftReleases"
        mavenContent { releasesOnly() }
    }
    maven(url = "https://repo.stellardrift.ca/repository/snapshots/") {
        name = "stellardriftSnapshots"
        mavenContent { snapshotsOnly() }
    }
}

val targetVersion = 16
java {
    sourceCompatibility = JavaVersion.toVersion(targetVersion)
    targetCompatibility = sourceCompatibility
    if (JavaVersion.current() < JavaVersion.toVersion(targetVersion)) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetVersion))
    }
}

tasks.withType(JavaCompile::class) {
    options.release.set(targetVersion)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnVersion")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("com.terraformersmc:modmenu:$modmenuVersion")
    modImplementation("net.earthcomputer.multiconnect:multiconnect-api:$multiconnectVersion")
    // https://github.com/IrisShaders/Iris/tree/1.17, published manually
    modImplementation("net.coderbot.iris_mc1_17:iris:1.1.0+rev.d3beab3")

    // for development
    modRuntime("com.sk89q.worldedit:worldedit-fabric-mc1.17.1:7.2.6-SNAPSHOT")
    runtimeOnly("net.minecraftforge:forgeflower:1.5.498.12")
}

tasks.withType(net.fabricmc.loom.task.AbstractRunTask::class).configureEach {
    // Mixin debug options
    jvmArgs(
        // "-Dmixin.debug.verbose=true",
        // "-Dmixin.debug.export=true",
        // "-Dmixin.debug.export.decompile.async=false", // to get decompiled sources when mixins straight up fail to apply
        "-Dmixin.dumpTargetOnFailure=true",
        "-Dmixin.checks.interfaces=true",
        "-Dwecui.debug.mixinaudit=true",
        "-Doptifabric.extract=true"
    )

    // Configure mixin agent
    jvmArgumentProviders += CommandLineArgumentProvider {
        // Resolve the Mixin configuration
        // Java agent: the jar file for mixin
        val mixinJar = configurations.runtimeClasspath.get().resolvedConfiguration
            .getFiles { it.name == "sponge-mixin" && it.group == "net.fabricmc" }
            .firstOrNull()

        if (mixinJar != null) {
            listOf("-javaagent:$mixinJar")
        } else {
            emptyList()
        }
    }
}

tasks.processResources.configure {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
