pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
}

rootProject.name = "semver.kt"

include(
    "release",
    "semantic-versioning",
    "spec",
)

