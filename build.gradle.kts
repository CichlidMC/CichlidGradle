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

val mcVer = "1.17.1"

dependencies {
    implementation(minecraft.client(mcVer))
    // compile against Cichlid API
//    compileOnly(cichlid.loaderApi("0.1.0"))
    // run with full Cichlid
//    runtimeOnly(cichlid.loader("0.1.0"))
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
