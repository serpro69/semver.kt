@file:Suppress("unused")

package io.github.serpro69.semverkt

import java.util.regex.Pattern
import kotlin.math.sign

class Semver(private val version: String) : Comparable<Semver> {
    private val versionPattern: Pattern =
        Pattern.compile("""^((?:\d+\.?){3})(-(?:[0-9A-Za-z-]*.?)*)?(\+(?:[0-9A-Za-z-]*.?)*)?${'$'}""")
    val normalVersion: String = version.substringBefore("-").substringBefore("+")
    val preRelease: PreRelease?
    val buildMetadata: BuildMetadata?
    val major: Int
    val minor: Int
    val patch: Int

    init {
        if (!isValid()) throw IllegalVersionException("'$version' is not a valid semver version.")
        normalVersion.split(".").also {
            major = it[0].toInt()
            minor = it[1].toInt()
            patch = it[2].toInt()
        }
        PreRelease(version.substringAfter("-", "").takeWhile { it != '+' }).also {
            preRelease = if (it.value == "") null else it
        }
        BuildMetadata(version.substringAfter("+", "")).also {
            buildMetadata = if (it.value == "") null else it
        }
    }

    constructor(
        major: Int,
        minor: Int,
        patch: Int
    ) : this("$major.$minor.$patch")

    constructor(
        major: Int,
        minor: Int,
        patch: Int,
        preRelease: PreRelease
    ) : this("$major.$minor.$patch-$preRelease")

    constructor(
        major: Int,
        minor: Int,
        patch: Int,
        buildMetadata: BuildMetadata
    ) : this("$major.$minor.$patch+$buildMetadata")

    constructor(
        major: Int,
        minor: Int,
        patch: Int,
        preRelease: PreRelease,
        buildMetadata: BuildMetadata
    ) : this("$major.$minor.$patch-$preRelease+$buildMetadata")

    fun incrementMajor() = Semver(major + 1, 0, 0)

    fun incrementMinor() = Semver(major, minor + 1, 0)

    fun incrementPatch() = Semver(major, minor, patch + 1)

    override operator fun compareTo(other: Semver): Int {
        val compareNormal = if (major > other.major) 1 else if (major < other.major) -1 else {
            if (minor > other.minor) 1 else if (minor < other.minor) -1 else {
                if (patch > other.patch) 1 else if (patch < other.patch) -1 else 0
            }
        }
        return when (compareNormal) {
            0 -> {
                when {
                    preRelease != null -> if (other.preRelease != null) preRelease.compareTo(other.preRelease) else -1
                    other.preRelease != null -> 1
                    else -> compareNormal
                }
            }
            else -> compareNormal
        }
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Semver -> major == other.major && minor == other.minor && patch == other.patch && preRelease == other.preRelease
            else -> false
        }
    }

    override fun toString(): String = version

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (preRelease?.hashCode() ?: 0)
        result = 31 * result + (buildMetadata?.hashCode() ?: 0)
        return result
    }

    private fun isValid(): Boolean {
        normalVersion.split(".").map {
            when {
                it.length > 1 && it.startsWith("0") -> {
                    throw IllegalVersionException("'$version' numbers MUST NOT contain leading zeroes.")
                }
                it.toInt().sign == -1 -> {
                    throw IllegalVersionException("'$version' numbers MUST NOT not be negative.")
                }
                else -> { /*NOOP*/
                }
            }
        }

        return version.matches(versionPattern.toRegex())
    }
}

class PreRelease internal constructor(val value: String) : Comparable<PreRelease> {
    override fun toString(): String = value

    override operator fun compareTo(other: PreRelease): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PreRelease -> value == other.value
            else -> false
        }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

class BuildMetadata internal constructor(val value: String) : Comparable<BuildMetadata> {
    override fun toString(): String = value

    override operator fun compareTo(other: BuildMetadata): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is BuildMetadata -> value == other.value
            else -> false
        }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
