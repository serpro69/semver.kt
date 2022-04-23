package io.github.serpro69.semverkt.release.configuration

interface VersionConfig {
    val preReleaseId: String get() = "rc"
}
