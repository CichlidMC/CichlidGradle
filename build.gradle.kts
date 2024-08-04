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
        distribution = "client"
    }
    runs {
        create("test") {
            copyFrom("server")
        }

        configureEach {
            jvmArgs.add("-Dmixin.debug.export=true")
        }
    }
}