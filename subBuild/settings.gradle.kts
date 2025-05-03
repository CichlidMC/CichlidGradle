rootProject.name = "sub-build-test"
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    repositories {
        // all dependencies need to be available too
        mavenCentral()
        maven("https://mvn.devos.one/releases")
        maven("https://libraries.minecraft.net")
    }

    includeBuild("../plugin")
}
