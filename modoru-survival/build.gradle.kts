plugins {
    id("com.gradleup.shadow").version("9.4.3")
}

repositories {
}

dependencies {
    paperweight.foliaDevBundle(property("paper_version") as String)
    implementation("com.github.justlofe:FastBytes:${property("fastbytes_version")}")
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
}