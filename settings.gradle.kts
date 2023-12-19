import io.github.serpro69.semverkt.gradle.plugin.SemverPluginExtension

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("io.github.serpro69.semantic-versioning") version "0.8.0"
}

rootProject.name = "semver.kt"

include(
    "release",
    "semantic-versioning",
    "spec",
)

settings.extensions.configure<SemverPluginExtension>("semantic-versioning") {
    git {
        message {
            preRelease = "[rc]"
            ignoreCase = true
        }
    }
    monorepo {
        module("release") {}
        module("semantic-versioning") {}
        module("spec") {}
    }
}
