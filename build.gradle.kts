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
        toolchain.languageVersion.set(JavaLanguageVersion.of(properties["java"]!! as String))
    }

    version = properties["version"]!!

    dependencies {
        compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
        compileOnly("dev.jorel:commandapi-paper-core:11.0.0")
        compileOnly("com.github.modoruru:hitori:${properties["hitori_version"]}")
        compileOnly("com.github.modoruru:hitori-resourcepack:${properties["resourcepack_version"]}")
        compileOnly("com.github.modoruru:hitori-ux:${properties["ux_version"]}")
    }
}
