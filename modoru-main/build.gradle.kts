plugins {
    id("com.gradleup.shadow").version("9.6.1")
}

dependencies {
    paperweight.foliaDevBundle(property("paper_version") as String)

    implementation("fun.lofe:stop-spying-on-your-players-messages:1.0.0+26.3")

    implementation("io.netty:netty-codec-native-quic:4.2.19.Final")
    implementation("fun.modoru:musubi:${property("musubi_version")}")
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier = ""
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