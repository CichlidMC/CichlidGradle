plugins {
    id("java-gradle-plugin")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://libraries.minecraft.net") }
}

dependencies {
    implementation(project(":pistonmeta"))
}

gradlePlugin {
    plugins {
        create("cichlidGradle") {
            id = "$group.cichlid_gradle"
            implementationClass = "io.github.cichlidmc.cichlid_gradle.CichlidGradlePlugin"
        }
    }
}
