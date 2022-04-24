package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver

interface VersionConfig {
    val initialVersion: Semver get() = Semver("0.1.0")
    val defaultIncrement: Increment get() = Increment.MINOR
    val preReleaseId: String get() = "rc"
    val initialPreRelease: Int get() = 1
    val snapshotSuffix: String get() = "SNAPSHOT"
}
