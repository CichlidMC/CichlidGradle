plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven { setUrl("https://libraries.minecraft.net") }
}

dependencies {
    api("com.mojang:datafixerupper:7.0.13")
    api("com.google.code.gson:gson:2.10.1")
}
