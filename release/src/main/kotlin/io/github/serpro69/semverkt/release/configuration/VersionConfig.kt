package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver

/**
 * Semantic Version configuration.
 *
 * @property initialVersion     the initial version that will be created if no versions exist
 * @property placeholderVersion the placeholder for the version that is used when no real version is set
 * @property defaultIncrement   the version element to increment by default, when no explicit increment is specified
 * @property preReleaseId       the pre-release identifier string
 * @property initialPreRelease  the initial number for new pre-release versions
 * @property snapshotSuffix     the snapshot version suffix
 */
interface VersionConfig {
    val initialVersion: Semver get() = Semver("0.1.0")
    val placeholderVersion: Semver get() = Semver("0.0.0")
    val defaultIncrement: Increment get() = Increment.MINOR
    val preReleaseId: String get() = "rc"
    val initialPreRelease: Int get() = 1
    val snapshotSuffix: String get() = "SNAPSHOT"

    /**
     * Returns a json string representation of this [VersionConfig] instance.
     */
    fun jsonString(): String {
        return """
            "version": { "initialVersion": "$initialVersion", "placeholderVersion": "$placeholderVersion", "defaultIncrement": "$defaultIncrement", "preReleaseId": "$preReleaseId", "initialPreRelease": "$initialPreRelease", "snapshotSuffix": "$snapshotSuffix" }
        """.trimIndent()
    }
}
