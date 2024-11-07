plugins {
    id("java")
    id("io.github.cichlidmc.cichlid_gradle")
    id("maven-publish")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    cichlid.snapshots()
    minecraft.libraries()
    minecraft.pistonMeta()
}

dependencies {
    implementation(minecraft.client("1.21"))
    // compile against Cichlid API
    compileOnly(cichlid.loaderApi("0.1.0"))
    // run with full Cichlid
    runtimeOnly(cichlid.loader("0.1.0"))
}

cichlid {
    runs {
        create("serverButCooler") {
            parent = "server"
            programArgs.get().remove("nogui")
            jvmArg("-Dmixin.debug.export=true")
        }

        configureEach {
            jvmArg("-Dmixin.debug.export=true")
        }
    }
}
