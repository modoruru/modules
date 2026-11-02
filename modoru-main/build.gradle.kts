plugins {
    id("com.gradleup.shadow").version("9.6.1")
}

dependencies {
    paperweight.foliaDevBundle(property("paper_version") as String)
    implementation("io.netty:netty-codec-native-quic:4.2.19.Final:linux-x86_64")

    implementation("fun.lofe:stop-spying-on-your-players-messages:1.0.0+26.3")
    implementation("fun.modoru:musubi:${property("musubi_version")}") {
        exclude("io.netty")
        exclude("org.jspecify")
    }
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier = ""

        eachFile {
            if (path.startsWith("META-INF/native/")) {
                println("NATIVE: $path")
            }
        }

        filesMatching("META-INF/native/libnetty_quiche42_linux_x86_64.so") {
            name = "libfun_modoru_shaded_netty_quiche42_linux_x86_64.so"
        }

        relocate("io.netty", "fun.modoru.shaded.io.netty")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("hitori.module.json") {
            expand(
                "version" to project.version,
                "java_version" to findProperty("java") as String,
                "hitori_version" to findProperty("hitori_version") as String,
                "resourcepack_version" to findProperty("resourcepack_version") as String
            )
        }
    }
}