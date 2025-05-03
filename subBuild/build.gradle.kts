plugins {
    id("java")
    id("fish.cichlidmc.cichlid-gradle")
    id("maven-publish")
}

group = "fish.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    cichlid.releases()
    minecraft.libraries()
    minecraft.versions()
}

val mc by minecraft.creating {
    version = "1.21.5"
}

dependencies {
    implementation(mc.dependency)

    compileOnly(cichlid.api("0.3.2"))
    cichlidRuntime(cichlid.runtime("0.3.2"))
}
