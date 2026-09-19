plugins {
    id("com.gradleup.shadow").version("9.4.3")
}

repositories {
}

dependencies {
    paperweight.foliaDevBundle(property("paper_version") as String)
    implementation("com.github.justlofe:FastBytes:${property("fastbytes_version")}")
    compileOnly(project(":modoru-main"))
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
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
                "main_version" to findProperty("main_version") as String,
                "resourcepack_version" to findProperty("resourcepack_version") as String
            )
        }
    }
}