import fish.cichlidmc.cichlid_gradle.util.Distribution

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

val mc by minecraft.creating {
    version = "1.21.7"
    distribution = Distribution.MERGED

    runs {
        register("server") {
            server()
        }
    }
}

dependencies {
    implementation(mc.dependency)

    mc.transformers.mod(files("src/main/resources"))
//    mc.transformers.namespaced("testmod")(files("src/main/resources/transformers"))

    compileOnly(cichlid.api("0.3.2"))
    cichlidRuntime(cichlid.runtime("0.3.2"))
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
