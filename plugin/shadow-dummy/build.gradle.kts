plugins {
    id("java-gradle-plugin")
}

// this dummy plugin project is needed because the plugin publish plugin doesn't recognize the new Shadow ID.

base.archivesName = "shadow"
group = "com.github.johnrengelman"
version = "dummy"

gradlePlugin {
    plugins {
        create("shadow") {
            id = "com.github.johnrengelman.shadow"
            implementationClass = "fish.cichlidmc.shadow_dummy.DummyPlugin"
        }
    }
}
