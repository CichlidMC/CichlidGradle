plugins {
    id("java-gradle-plugin")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://libraries.minecraft.net")
    maven("https://maven.neoforged.net")
}

dependencies {
    implementation("io.github.cichlidmc:PistonMetaParser:2.0.1")
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
