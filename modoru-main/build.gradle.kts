repositories {
}

dependencies {
    paperweight.foliaDevBundle(property("paper_version") as String)
    compileOnly("io.netty:netty-codec-native-quic:4.2.19.Final")
}

tasks {
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