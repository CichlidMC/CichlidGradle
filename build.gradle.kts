plugins {
    id("java-gradle-plugin")
}

group = "io.github.tropheusj"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://libraries.minecraft.net") }
}

dependencies {
    implementation("com.mojang:datafixerupper:7.0.13")
    implementation("com.google.code.gson:gson:2.10.1")
}

gradlePlugin {
    plugins {
        create("cichlidGradle") {
            id = "$group.cichlid-gradle"
            implementationClass = "io.github.tropheusj.cichlid_gradle.CichlidGradlePlugin"
        }
    }
}
