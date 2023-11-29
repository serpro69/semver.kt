package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.configuration.MonorepoConfig
import io.github.serpro69.semverkt.release.configuration.VersionConfig
import io.github.serpro69.semverkt.release.repo.Commit
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Message
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.release.repo.semver
import io.github.serpro69.semverkt.spec.PreRelease
import io.github.serpro69.semverkt.spec.Semver

/**
 * Provides functions for semantic releases of a project [Repository].
 *
 * @property currentVersion the current version in a given repository
 * @property latestVersion the last version in a given repository
 */
class SemverRelease : AutoCloseable {
    private val repo: Repository
    private val config: Configuration

    val currentVersion: () -> Semver?
    val latestVersion: () -> Semver?

    /**
     * @constructor Creates an instance of [SemverRelease] with given [configuration] parameters.
     */
    constructor(configuration: Configuration) {
        repo = GitRepository(configuration)
        config = configuration
        currentVersion = { repo.headVersionTag()?.let { semver(configuration.git.tag)(it) } }
        latestVersion = { repo.latestVersionTag()?.let { semver(configuration.git.tag)(it) } }
    }

    /**
     * @constructor Creates an instance of [SemverRelease] for a given [repository].
     */
    constructor(repository: Repository) {
        repo = repository
        config = repo.config
        currentVersion = { repo.headVersionTag()?.let { semver(config.git.tag)(it) } }
        latestVersion = { repo.latestVersionTag()?.let { semver(config.git.tag)(it) } }
    }

    override fun close() {
        repo.close()
    }

    /**
     * Returns the [version] IF it's moreThan [latestVersion] AND not already present in the [repo],
     * ELSE returns `null`.
     */
    fun release(version: Semver): Semver? {
        val moreThanLatest = latestVersion()?.let { version > it } ?: true
        val exists by lazy { repo.tags().map { semver(config.git.tag)(it) }.any { it == version } }
        return if (moreThanLatest && !exists) version else null
    }

    /**
     * Returns the next release version after the [latestVersion] based on the [increment].
     *
     * See also [SemverRelease.increment] for the rules that are used when releasing the versions.
     */
    fun release(increment: Increment): Semver = increment(increment, true)

    fun releaseModules(increment: Increment, monorepoConfig: MonorepoConfig): Pair<Semver, List<String>> = increment(increment, true) to emptyList()

    /**
     * Returns the next snapshot version after the [latestVersion] based on the [increment].
     *
     * See also [SemverRelease.increment] for the rules that are used when creating version snapshots.
     */
    fun snapshot(increment: Increment): Semver = increment(increment, false)

    /**
     * Increment the [latestVersion] to the next [Semver] using the [increment].
     *
     * The following rules apply when incrementing the [latestVersion]:
     *
     * 1. **IF the [latestVersion] is `null`.**
     * THEN initial version is returned as configured by [VersionConfig.initialVersion].
     *
     * 2. **The [increment] is a [Increment.PRE_RELEASE].**
     * IF [latestVersion] is a pre-release version,
     * THEN the [Semver.preRelease] component is incremented,
     * ELSE the [latestVersion] is returned.
     *
     * 3. **The [increment] is [Increment.DEFAULT].**
     * IF [latestVersion] is a pre-release version,
     * THEN the [Semver.preRelease] component is incremented,
     * ELSE the [latestVersion] is incremented using [VersionConfig.defaultIncrement].
     *
     * 4. **If the [increment] is [Increment.NONE],**
     * THEN the [latestVersion] is returned.
     *
     * @param increment the version component to increment
     * @param release   if `false` appends [VersionConfig.snapshotSuffix] to the final version
     */
    fun increment(
        increment: Increment,
        release: Boolean = (increment != Increment.DEFAULT),
    ): Semver {
        val nextVersion = latestVersion()?.let {
            when (increment) {
                Increment.MAJOR -> it.incrementMajor()
                Increment.MINOR -> it.incrementMinor()
                Increment.PATCH -> it.incrementPatch()
                Increment.PRE_RELEASE -> {
                    with(it.incrementPreRelease()) {
                        preRelease?.let { _ -> if (release) this else withSnapshot() } ?: this
                    }
                }
                Increment.DEFAULT -> {
                    it.preRelease?.let { _ -> increment(Increment.PRE_RELEASE, release) }
                        ?: increment(config.version.defaultIncrement, release)
                }
                Increment.NONE -> latestVersion()
            }
        } ?: config.version.initialVersion
        return if (release || increment == Increment.PRE_RELEASE) nextVersion else nextVersion.withSnapshot()
    }

    /**
     * Creates a pre-release version with a given [increment], and returns as [Semver] instance.
     *
     * The following rules apply when creating a new pre-release version:
     *
     * 1. **WHEN [latestVersion] is a pre-release version.**
     * IF the [latestVersion] is a snapshot version (ends with the [VersionConfig.snapshotSuffix]),
     * OR the [increment] is [Increment.PRE_RELEASE] or [Increment.NONE]
     * THEN the [latestVersion] is returned,
     * ELSE increases the [latestVersion] to the next [increment],
     * and appends the [PreRelease] to it with the [VersionConfig.initialPreRelease] number.
     *
     * 2. **WHEN [latestVersion] is `null`,**
     * THEN a new pre-release version consisting of [VersionConfig.initialVersion] and [VersionConfig.initialPreRelease] is returned,
     *
     * 3. **WHEN [latestVersion] is a release version.**
     * IF [increment] is one of [Increment.MAJOR], [Increment.MINOR], [Increment.PATCH] or [Increment.DEFAULT],
     * THEN increases the [latestVersion] to the next [increment]
     *   and appends the [PreRelease] to it with the [VersionConfig.initialPreRelease] number,
     * ELSE the [latestVersion] is returned.
     */
    fun createPreRelease(increment: Increment): Semver {
        val preRelease = PreRelease("${config.version.preReleaseId}.${config.version.initialPreRelease}")
        return with(latestVersion()) {
            this?.preRelease?.let {
                if (this.isSnapshot() || increment in listOf(Increment.PRE_RELEASE, Increment.NONE)) this else {
                    val nextVer = if (increment == Increment.DEFAULT) {
                        release(config.version.defaultIncrement)
                    } else release(increment)
                    nextVer.copy(preRelease = preRelease)
                }
            } ?: run {
                if (increment in listOf(Increment.PRE_RELEASE, Increment.NONE)) {
                    this ?: release(increment).copy(preRelease = preRelease)
                } else {
                    release(increment).copy(preRelease = preRelease)
                }
            }
        }
    }

    private fun Semver.isSnapshot(): Boolean = toString().endsWith("-${config.version.snapshotSuffix}")

    /**
     * Promotes a pre-release version to a release version.
     *
     * IF [latestVersion] is a pre-release version,
     * THEN a copy of the [latestVersion] is returned as a normal version (with the pre-release component stripped),
     * ELSE the [latestVersion] version is returned.
     */
    fun promoteToRelease(): Semver? {
        return with(latestVersion()) {
            this?.preRelease?.let { copy(preRelease = null, buildMetadata = null) } ?: this
        }
    }

    /**
     * Returns the next [Increment] based on the [Repository.latestVersionTag].
     *
     * IF any of the commits contain a keyword for incrementing a specific version, as configured by [GitMessageConfig],
     * THEN that increment will be returned based on the precedence rules,
     * ELSE [Increment.NONE] is returned.
     *
     * IF the repository `HEAD` points to the [Repository.latestVersionTag],
     * OR the repository `HEAD` points to any other existing release tag, as configured by [GitTagConfig.prefix],
     * THEN [Increment.NONE] is returned.
     *
     * Precedence rules for commit message keywords, from highest to lowest:
     * - [GitMessageConfig.major]
     * - [GitMessageConfig.minor]
     * - [GitMessageConfig.patch]
     * - [GitMessageConfig.preRelease]
     */
    fun nextIncrement(): Increment {
        val head = repo.head()
        val latestTag = repo.latestVersionTag()
        val tagObjectId = latestTag?.peeledObjectId ?: latestTag?.objectId
        val isHeadOnTag by lazy { repo.tags().any { head == it.peeledObjectId || head == it.objectId } }
        return if (head == tagObjectId || isHeadOnTag) Increment.NONE else repo.log(latestTag).nextIncrement()
    }

    private fun List<Commit>.nextIncrement(): Increment {
        var inc = Increment.DEFAULT
        forEach { c ->
            if (c.message.full().contains(config.git.message.major)) return Increment.MAJOR
            if (inc > Increment.MINOR && c.message.full().contains(config.git.message.minor)) {
                inc = Increment.MINOR
                return@forEach
            }
            if (inc > Increment.PATCH && c.message.full().contains(config.git.message.patch)) {
                inc = Increment.PATCH
                return@forEach
            }
            if (inc > Increment.PRE_RELEASE && c.message.full().contains(config.git.message.preRelease)) {
                inc = Increment.PRE_RELEASE
            }
        }
        return inc
    }

    private fun Semver.withSnapshot(): Semver {
        val pr = preRelease?.toString()?.let {
            when {
                it.endsWith(config.version.snapshotSuffix) -> it
                else -> "${it}-${config.version.snapshotSuffix}"
            }
        } ?: config.version.snapshotSuffix
        return copy(preRelease = PreRelease(pr), buildMetadata = null)
    }

    private fun Message.full(): String {
        return with(StringBuilder()) {
            appendLine(title)
            appendLine()
            appendLine(description.joinToString("\n"))
            toString()
        }
    }
}

