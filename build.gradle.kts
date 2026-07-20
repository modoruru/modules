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

    version = property("version")!!

    dependencies {
        compileOnly("io.papermc.paper:paper-api:${property("paper_version")}")
        compileOnly("dev.jorel:commandapi-paper-core:${property("commandapi_version")}")
        compileOnly("com.github.modoruru:hitori:${property("hitori_version")}")
        compileOnly("com.github.modoruru:hitori-resourcepack:${property("resourcepack_version")}")
        compileOnly("com.github.modoruru:hitori-ux:${property("ux_version")}")
    }
}
