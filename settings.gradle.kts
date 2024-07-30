rootProject.name = "CichlidGradleTest"
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net")
    }

    includeBuild("plugin")
}
