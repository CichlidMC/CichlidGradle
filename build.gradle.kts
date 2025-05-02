plugins {
    id("java")
    id("fish.cichlidmc.cichlid-gradle")
    id("maven-publish")
}

group = "fish.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    cichlid.releases()
    minecraft.libraries()
    minecraft.versions()
}

val mcVer = "1.21.5"

dependencies {
    implementation(minecraft.of {
        client()
        version(mcVer)
    })

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

tasks.register("resolveCompileSources") {
    val view = configurations.compileClasspath.get().incoming.artifactView {
       componentFilter {
           it.displayName.contains("minecraft")
       }

        withVariantReselection()

        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }.artifacts

    doLast {
        view.artifactFiles.forEach {
            println(it)
        }
    }
}
