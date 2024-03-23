import io.github.serpro69.semverkt.gradle.plugin.SemverPluginExtension
import io.github.serpro69.semverkt.release.configuration.TagPrefix

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("io.github.serpro69.semantic-versioning") version "0.10.0"
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
        module(":release") {
            tag {
                prefix = TagPrefix("release-v")
            }
        }
        module(":semantic-versioning") {
            tag {
                prefix = TagPrefix("gradle-v")
            }
        }
        module(":spec") {
            tag {
                prefix = TagPrefix("spec-v")
            }
        }
    }
}
