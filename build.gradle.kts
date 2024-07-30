plugins {
    id("java")
    id("io.github.cichlidmc.cichlid_gradle")
    id("maven-publish")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("net.minecraft:minecraft-client:1.21")
    // compile against Cichlid API
    compileOnly("io.github.cichlidmc:CichlidApi:0.1.0")
    // run with full Cichlid
    runtimeOnly("io.github.cichlidmc:Cichlid:0.1.0")
}
