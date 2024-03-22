package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.spec.Semver

/**
 * Holds semantic versions of a given project
 *
 * @param headVersion the current version at project HEAD, if it exists, or `null` otherwise
 * @param latestVersion the latest project release version, if it exists, or `null` otherwise
 * @param nextVersion the (calculated) project next release version,
 * if it exists (given the current inputs and configuration), or `null` otherwise
 */
data class SemanticProject(
    val headVersion: HeadVersion?,
    val latestVersion: LatestVersion?,
    val nextVersion: NextVersion?,
) {
    /**
     * Creates an instance of this [SemanticProject] with [headVersion] set to `null`
     */
    constructor(latestVersion: LatestVersion?, nextVersion: NextVersion?) : this(headVersion = null, latestVersion = latestVersion, nextVersion = nextVersion)
}

inline fun <reified T : Version> Semver.toVersion(): T {
    return when (val t = T::class) {
        HeadVersion::class -> HeadVersion(this) as T
        LatestVersion::class -> LatestVersion(this) as T
        NextVersion::class -> NextVersion(this) as T
        else -> throw IllegalArgumentException("Unsupported version type: $t")
    }
}

sealed interface Version {
    val value: Semver

    operator fun compareTo(other: Version): Int {
        return value.compareTo(other.value)
    }

}

@JvmInline
value class HeadVersion(override val value: Semver) : Version {

    override fun toString(): String = value.toString()
}

@JvmInline
value class LatestVersion(override val value: Semver) : Version {

    override fun toString(): String = value.toString()
}

@JvmInline
value class NextVersion(override val value: Semver) : Version {

    override fun toString(): String = value.toString()
}
