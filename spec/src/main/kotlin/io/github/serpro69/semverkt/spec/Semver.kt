package io.github.serpro69.semverkt.spec

import java.util.regex.Pattern
import kotlin.math.sign

/**
 * Returns an instance of [Semver] from this receiver string.
 *
 * @param prefix a version prefix, for example when having a git tag `v1.2.3` representing a version, that should be stripped.
 */
fun String.toSemver(prefix: String? = null): Semver {
    return Semver(prefix?.let { replace(Regex("""^$prefix"""), "") } ?: this)
}

/**
 * Represents a semantic [version] as per [Semantic Versioning 2.0.0](https://semver.org/#semantic-versioning-200) specification.
 *
 * @property major          MAJOR version number identifier
 * @property minor          MINOR version number identifier
 * @property patch          PATCH version number identifier
 * @property normalVersion  the "normal version number" of this semantic [version] in the form of `X.Y.Z`
 * @property preRelease     the optional "pre-release identifier" of this semantic [version]
 * @property buildMetadata  the optional "build metadata identifier" of this semantic [version]
 *
 * @constructor             creates an instance of [Semver] where semantic version is constructed from the [version] string
 */
class Semver(private val version: String) : Comparable<Semver> {
    private val versionPattern: Pattern =
        Pattern.compile("""^((?:\d+\.){2}\d+)(-(?:[0-9A-Za-z-]*.?)*)?(\+(?:[0-9A-Za-z-]*.?)*)?${'$'}""")
    val normalVersion: String = version.substringBefore("+").substringBefore("-")
    val preRelease: PreRelease?
    val buildMetadata: BuildMetadata?
    val major: Int
    val minor: Int
    val patch: Int

    companion object {
        fun isValid(version: String): Boolean {
            return try {
                Semver(version).isValid()
            } catch (e: IllegalVersionException) {
                false
            }
        }
    }

    init {
        normalVersion.split(".").also {
            if (it.size != 3) throw IllegalVersionException("$version version number MUST take the form X.Y.Z")
            if (it.contains("")) throw IllegalVersionException("$version version number MUST not contain empty elements")
            try {
                major = it[0].toInt()
                minor = it[1].toInt()
                patch = it[2].toInt()
            } catch (e: NumberFormatException) {
                throw IllegalVersionException("Normal version of '$version' MUST only contain numbers")
            }
        }
        PreRelease(version.substringBefore("+").substringAfter("-", "").takeWhile { it != '+' }).also {
            preRelease = if (it.value == "") null else it
        }
        BuildMetadata(version.substringAfter("+", "")).also {
            buildMetadata = if (it.value == "") null else it
        }
        if (!isValid()) throw IllegalVersionException("'$version' is not a valid semver version.")
    }

    /**
     * @constructor creates an instance of [Semver] where semantic version is constructed from the [major], [minor], and [patch] version numbers.
     */
    constructor(
        major: Int,
        minor: Int,
        patch: Int
    ) : this("$major.$minor.$patch")

    /**
     * @constructor creates an instance of [Semver] where semantic version is constructed from the [major], [minor], and [patch] version numbers,
     * as well as [preRelease] version identifier.
     */
    constructor(
        major: Int,
        minor: Int,
        patch: Int,
        preRelease: PreRelease
    ) : this("$major.$minor.$patch-$preRelease")

    /**
     * @constructor creates an instance of [Semver] where semantic version is constructed from the [major], [minor], and [patch] version numbers,
     * as well as [buildMetadata] version identifier.
     */
    constructor(
        major: Int,
        minor: Int,
        patch: Int,
        buildMetadata: BuildMetadata
    ) : this("$major.$minor.$patch+$buildMetadata")

    /**
     * @constructor creates an instance of [Semver] where semantic version is constructed from the [major], [minor], and [patch] version numbers,
     * as well as [preRelease] and [buildMetadata] version identifiers.
     */
    constructor(
        major: Int,
        minor: Int,
        patch: Int,
        preRelease: PreRelease,
        buildMetadata: BuildMetadata
    ) : this("$major.$minor.$patch-$preRelease+$buildMetadata")

    /**
     * Increment this version to next [major] version.
     */
    fun incrementMajor() = Semver(major + 1, 0, 0)

    /**
     * Increment this version to next [minor] version.
     */
    fun incrementMinor() = Semver(major, minor + 1, 0)

    /**
     * Increment this version to next [patch] version.
     */
    fun incrementPatch() = Semver(major, minor, patch + 1)

    fun incrementPreRelease(): Semver = preRelease?.let { copy(preRelease = it.increment()) } ?: this

    /**
     * Returns a copy of [Semver] as a new instance
     * with optionally modified [major], [minor], [patch], [preRelease], and [buildMetadata] properties.
     */
    fun copy(
        major: Int = this.major,
        minor: Int = this.minor,
        patch: Int = this.patch,
        preRelease: PreRelease? = this.preRelease,
        buildMetadata: BuildMetadata? = this.buildMetadata
    ): Semver {
        return if (preRelease != null && buildMetadata != null) {
            Semver(major, minor, patch, preRelease, buildMetadata)
        } else if (preRelease != null) {
            Semver(major, minor, patch, preRelease)
        } else if (buildMetadata != null) {
            Semver(major, minor, patch, buildMetadata)
        } else {
            Semver(major, minor, patch)
        }
    }

    /**
     * Compares this semantic version with the [other] one.
     * Returns `0` if this version is equal to the specified [other] version,
     * a negative number if it's less than [other] version,
     * or a positive number if it's greater than the [other] version.
     */
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
                    else -> 0
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

    /**
     * Returns a string representation of this semantic version.
     */
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
        // Check if normal version starts with negative number. #2
        if (version.startsWith("-")) throw IllegalVersionException("'$version' numbers MUST NOT not be negative.")
        // Other checks for normal version
        normalVersion.split(".").forEach {
            when {
                // Check if version numbers start with leading 0. #2
                it.length > 1 && it.startsWith("0") -> throw IllegalVersionException("'$version' numbers MUST NOT contain leading zeroes.")
                // Check if any of normal version numbers are negative. #2
                it.toInt().sign == -1 -> throw IllegalVersionException("'$version' numbers MUST NOT not be negative.")
                else -> { /*NOOP*/
                }
            }
        }
        // Checks for pre-release
        preRelease?.validate()
        // Checks for build metadata
        buildMetadata?.validate()
        return version.matches(versionPattern.toRegex())
    }
}

/**
 * Represents an optional "pre-release identifier" of a semantic version.
 */
class PreRelease(internal val value: String) : Comparable<PreRelease> {

    internal fun increment(): PreRelease {
        val first = value.substringBeforeLast(".")
        val last = value.substringAfterLast(".")
        return if (last.all { c -> c.isDigit() }) PreRelease("$first.${last.toInt() + 1}") else this
    }

    internal fun validate() {
        // Check for empty identifiers in pre-release. #9
        if (value.contains("..")) throw IllegalVersionException("'$value' pre-release MUST NOT not contain empty identifiers.")
        // Check for leading zeroes in pre-release identifiers
        value.split(".").forEach { s ->
            when {
                // Check if numeric identifiers start with leading 0
                s.all { c -> c.isDigit() } && s.length > 1 && s.startsWith("0") -> {
                    throw IllegalVersionException("'$value' pre-release identifiers MUST NOT contain leading zeroes.")
                }
                else -> { /*NOOP*/
                }
            }
        }
    }

    /**
     * Returns a string representation of this [PreRelease]
     */
    override fun toString(): String = value

    override operator fun compareTo(other: PreRelease): Int {
        var r = 0
        val list = value.split(".")
        val otherList = other.value.split(".")
        for (p in list.zip(otherList)) {
            val (a, b) = p
            val aIsNum = a.all { it.isDigit() }
            val bIsNum = b.all { it.isDigit() }
            when {
                a == b -> continue
                aIsNum && bIsNum -> {
                    r = a.toInt().compareTo(b.toInt())
                    break
                }
                else -> {
                    r = a.compareTo(b)
                    break
                }
            }
        }
        return if (r != 0) r else if (list.size == otherList.size) 0 else if (list.size > otherList.size) 1 else -1
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

/**
 * Represents an optional "build metadata identifier" of a semantic version.
 */
class BuildMetadata(internal val value: String) : Comparable<BuildMetadata> {

    internal fun validate() {
        // Check for empty identifiers in build metadata. #10
        if (value.contains("..")) {
            throw IllegalVersionException("'$value' build metadata MUST NOT not contain empty identifiers.")
        }
    }

    /**
     * Returns a string representation of this [BuildMetadata]
     */
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
