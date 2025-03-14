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
    implementation(minecraft.client("1.17.1"))
    // compile against Cichlid API
//    compileOnly(cichlid.loaderApi("0.1.0"))
    // run with full Cichlid
//    runtimeOnly(cichlid.loader("0.1.0"))
}

cichlid {
    runs {
        register("client") {
            version = "1.17.1"
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
