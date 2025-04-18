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
    minecraft.versions()
}

val mcVer = "1.21.5"

dependencies {
    implementation(minecraft.client(mcVer))

    compileOnly(cichlid.api("0.3.2"))
    cichlidRuntime(cichlid.runtime("0.3.2"))
}

cichlid {
    runs {
        register("client") {
            version = mcVer
        }
//        register("client 1.21.4") {
//            version = "1.21.4"
//            template = "client"
//        }

//        register("serverButCooler") {
//            template = "server"
//            programArgs.get().remove("nogui")
//            jvmArg("-Dmixin.debug.export=true")
//        }
//
//        configureEach {
//            version = minecraftVersion
//            jvmArg("-Dmixin.debug.export=true")
//        }
    }
}
