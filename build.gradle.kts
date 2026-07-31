plugins {
    java
    id("io.papermc.paperweight.userdev").version("2.0.0-beta.21").apply(false)
}

allprojects {
    repositories {
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }

        maven("https://repository.modoru.fun/releases") {
            name = "modoruReleases"
        }

        maven("https://jitpack.io") {
            name = "jitpack"
        }
    }
}

subprojects  {
    apply(plugin = "java")
    apply(plugin = "io.papermc.paperweight.userdev")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(property("java")!! as String))
    }

    // removes "modoru-" from the project name and adds "_version". example: "modoru-main" -> "main" -> "main_version"
    version = property(project.name.substring(7) + "_version")!!

    dependencies {
        compileOnly("dev.folia:folia-api:${property("paper_version")}")
        compileOnly("su.hitori:hitori:${property("hitori_version")}")
        compileOnly("su.hitori:hitori-resourcepack:${property("resourcepack_version")}")
        compileOnly("su.hitori.ux:module:${property("ux_version")}")
    }
}
