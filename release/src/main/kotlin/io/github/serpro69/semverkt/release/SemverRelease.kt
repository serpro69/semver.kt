package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
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
 * @property currentVersion the current (last) version in a given repository
 */
class SemverRelease {
    private val repo: Repository
    private val config: Configuration

    val currentVersion: () -> Semver?

    /**
     * @constructor Creates an instance of [SemverRelease] with given [configuration] parameters.
     */
    constructor(configuration: Configuration) {
        repo = GitRepository(configuration)
        config = configuration
        currentVersion = { repo.latestVersionTag()?.let { semver(configuration.git.tag)(it) } }
    }

    /**
     * @constructor Creates an instance of [SemverRelease] for a given [repository].
     */
    constructor(repository: Repository) {
        repo = repository
        config = repo.config
        currentVersion = { repo.latestVersionTag()?.let { semver(config.git.tag)(it) } }
    }

    /**
     * Returns the next release version after the [currentVersion] based on the [increment].
     */
    fun release(increment: Increment): Semver = increment(increment, true)

    /**
     * Returns the next snapshot version after the [currentVersion] based on the [increment].
     */
    fun snapshot(increment: Increment): Semver = increment(increment, false)

    /**
     * Increment the [currentVersion] to the next [Semver] using the [increment].
     *
     * @param release if `false` appends [VersionConfig.snapshotSuffix] to the final version
     */
    fun increment(
        increment: Increment,
        release: Boolean = (increment != Increment.DEFAULT),
    ): Semver {
        val nextVersion = currentVersion()?.let {
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
                Increment.NONE -> currentVersion()
            }
        } ?: config.version.initialVersion
        return if (release || increment == Increment.PRE_RELEASE) nextVersion else nextVersion.withSnapshot()
    }

    /**
     * Creates a pre-release version with an [increment] increment, and returns as [Semver] instance.
     *
     * IF [currentVersion] is already a pre-release version, returns the [currentVersion],
     * ELSE [release]s the [currentVersion] to the next [increment]
     * and appends the [PreRelease] to it with the [VersionConfig.initialPreRelease] number.
     */
    fun createPreRelease(increment: Increment): Semver {
        return currentVersion()?.preRelease?.let { currentVersion() } ?: run {
            val preRelease = PreRelease("${config.version.preReleaseId}.${config.version.initialPreRelease}")
            release(increment).copy(preRelease = preRelease)
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
        var inc = Increment.NONE
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

