plugins {
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.gradleup.shadow") version "8.3.0"
}

base.archivesName = "CichlidGradle"
group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://mvn.devos.one/snapshots")
    maven("https://maven.neoforged.net")
}

dependencies {
    implementation("io.github.cichlidmc:PistonMetaParser:2.0.1")
    implementation("net.neoforged:AutoRenamingTool:2.0.3")
}

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    relocate("net.neoforged", "io.github.cichlidmc.shadow.net.neoforged")
}

gradlePlugin {
    website = "https://cichlidmc.github.io/"
    vcsUrl = "https://github.com/CichlidMC/CichlidGradle"

    plugins {
        create("cichlidGradle") {
            id = "$group.cichlid_gradle"
            displayName = "Cichlid Gradle"
            description = "Gradle plugin for developing Minecraft mods with Cichlid"
            tags = setOf("java", "minecraft", "mod", "modding", "cichlid", "cichlidmc", "mcdev", "minecraft-dev", "minecraft-modding")
            implementationClass = "io.github.cichlidmc.cichlid_gradle.CichlidGradlePlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven("https://mvn.devos.one/snapshots") {
            name = "devOS"
            credentials(PasswordCredentials::class)
        }
    }
}
