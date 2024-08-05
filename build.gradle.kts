plugins {
    id("java")
    id("io.github.cichlidmc.cichlid_gradle")
    id("maven-publish")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // compile against Cichlid API
    compileOnly("io.github.cichlidmc:CichlidApi:0.1.0")
    // run with full Cichlid
    runtimeOnly("io.github.cichlidmc:Cichlid:0.1.0")
}

cichlid {
    minecraft {
        version = "1.21"
        distribution = "server"
    }
    runs {
        create("test") {
//            parent = "client"
//            jvmArgs.get().add("")
        }

        configureEach {
//            jvmArgs.add("-Dmixin.debug.export=true")
        }
    }
}
