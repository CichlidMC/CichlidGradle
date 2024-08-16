rootProject.name = "CichlidGradleTest"
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://mvn.devos.one/snapshots")
        maven("https://libraries.minecraft.net")
    }

    includeBuild("plugin")
}
