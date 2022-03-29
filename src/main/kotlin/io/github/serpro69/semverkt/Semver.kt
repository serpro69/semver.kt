@file:Suppress("unused")

package io.github.serpro69.semverkt

import kotlin.math.sign

class Semver(private val version: String) : Comparable<Semver> {
    val normalVersion: Semver
    val preReleaseVersion: PreRelease
    val buildMetadata: BuildMetadata
    val major: Int
    val minor: Int
    val patch: Int

    init {
        if (!isValid()) throw IllegalVersionException("'$version' is not a valid semver version.")
        normalVersion = Semver(version.substringBefore("-").substringBefore("+"))
        preReleaseVersion = PreRelease(version.substringAfter("-").takeWhile { it != '+' })
        buildMetadata = BuildMetadata(version.substringAfter("+"))
        val versions = normalVersion.version.split(".")
        major = versions[0].toInt()
        minor = versions[1].toInt()
        patch = versions[2].toInt()
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
        TODO("Not yet implemented")
    }

    override fun toString(): String = version

    private fun isValid(): Boolean {
        val elements = version.split(".")
            .map {
                when {
                    it.length > 1 && it.startsWith("0") -> {
                        throw IllegalVersionException("'$version' numbers MUST NOT contain leading zeroes.")
                    }
                    else -> it.toInt()
                }
            }
        return elements.none { it.sign == -1 } && elements.size == 3
    }
}

class PreRelease internal constructor(val value: String) : Comparable<PreRelease> {
    override fun toString(): String = value

    override fun compareTo(other: PreRelease): Int {
        TODO("Not yet implemented")
    }
}

class BuildMetadata internal constructor(val value: String): Comparable<PreRelease> {
    override fun toString(): String = value

    override fun compareTo(other: PreRelease): Int {
        TODO("Not yet implemented")
    }
}
