plugins {
    id("java")
    id("io.github.cichlidmc.cichlid_gradle")
    id("maven-publish")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://mvn.devos.one/snapshots")
}

dependencies {
    minecraft {
        version = "1.21"
        distribution = "server"
    }
    // compile against Cichlid API
    compileOnly("io.github.cichlidmc:CichlidApi:0.1.0")
    // run with full Cichlid
    runtimeOnly("io.github.cichlidmc:Cichlid:0.1.0")
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
