package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.spec.Semver

interface VersionConfig {
    val initialVersion: Semver get() = Semver("0.1.0")
    val preReleaseId: String get() = "rc"
}
