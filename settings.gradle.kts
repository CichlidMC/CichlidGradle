rootProject.name = "CichlidGradleTest"
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    repositories {
        mavenCentral()
        maven { setUrl("https://libraries.minecraft.net") }
    }

    includeBuild("plugin")
}
