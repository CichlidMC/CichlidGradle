plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.0"
    id("com.github.johnrengelman.shadow") version "dummy"
}

group = "fish.cichlidmc"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://mvn.devos.one/releases")
    maven("https://maven.neoforged.net")
}

dependencies {
    implementation("fish.cichlidmc:distribution-marker:1.0.1")
    implementation("fish.cichlidmc:piston-meta-parser:2.0.2")
    implementation("fish.cichlidmc:sushi:0.1.0")
    implementation("net.neoforged:AutoRenamingTool:2.0.3")
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("org.vineflower:vineflower:1.11.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier = ""
    relocate("net.neoforged", "fish.cichlidmc.shadow.net.neoforged")
}

gradlePlugin {
    website = "https://cichlidmc.fish/"
    vcsUrl = "https://github.com/CichlidMC/cichlid-gradle"

    plugins {
        create("cichlidGradle") {
            id = "$group.cichlid-gradle"
            displayName = "Cichlid Gradle"
            description = "Gradle plugin for developing Minecraft mods with Cichlid"
            tags = setOf("java", "minecraft", "mod", "modding", "cichlid", "cichlidmc", "mcdev", "minecraft-dev", "minecraft-modding")
            implementationClass = "fish.cichlidmc.cichlid_gradle.CichlidGradlePlugin"
        }
    }
}

publishing {
    repositories {
        listOf("Releases", "Snapshots").forEach {
            maven("https://mvn.devos.one/${it.lowercase()}") {
                name = "devOs$it"
                credentials(PasswordCredentials::class)
            }
        }
    }
}
