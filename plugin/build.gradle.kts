plugins {
    id("java-gradle-plugin")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
    maven("https://maven.neoforged.net")
}

dependencies {
    implementation(project(":pistonmeta"))
    implementation("net.neoforged:AutoRenamingTool:2.0.3")
}

gradlePlugin {
    plugins {
        create("cichlidGradle") {
            id = "$group.cichlid_gradle"
            implementationClass = "io.github.cichlidmc.cichlid_gradle.CichlidGradlePlugin"
        }
    }
}
