plugins {
    id("java-gradle-plugin")
}

group = "io.github.tropheusj"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

gradlePlugin {
    plugins {
        create("cichlidGradle") {
            id = "$group.cichlid-gradle"
            implementationClass = "io.github.tropheusj.cichlid_gradle.CichlidGradlePlugin"
        }
    }
}