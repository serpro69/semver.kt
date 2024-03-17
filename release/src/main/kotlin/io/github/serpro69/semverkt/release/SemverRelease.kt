package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.configuration.ModuleConfig
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.release.configuration.VersionConfig
import io.github.serpro69.semverkt.release.repo.Commit
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Message
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.release.repo.semver
import io.github.serpro69.semverkt.spec.PreRelease
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.lib.Ref

/**
 * Provides functions for semantic releases of a project [Repository].
 *
 * @property currentVersion the current version in a this [repo],
 * optionally for a given module configured via [ModuleConfig] in the [config] instance.
 *
 * @property latestVersion  the last version in this [repo],
 * optionally for a given module configured via [ModuleConfig] in the [config] instance.
 */
class SemverRelease : AutoCloseable {
    private val repo: Repository
    private val config: Configuration
    private val sv: (ModuleConfig?) -> (Ref) -> Semver
    private val prefix: (Configuration, ModuleConfig?) -> TagPrefix = { c, m -> m?.tag?.prefix ?: c.git.tag.prefix }

    val currentVersion: (module: ModuleConfig?) -> Semver?
    val latestVersion: (module: ModuleConfig?) -> Semver?

    /**
     * @constructor Creates an instance of [SemverRelease] with given [configuration] parameters.
     */
    constructor(configuration: Configuration) {
        config = configuration
        repo = GitRepository(config)
        sv = { m -> semver(prefix(config, m)) }
        currentVersion = { m -> repo.headVersionTag(prefix(config, m))?.let { sv(m)(it) } }
        latestVersion = { m -> repo.latestVersionTag(prefix(config, m))?.let { sv(m)(it) } }
    }

    /**
     * @constructor Creates an instance of [SemverRelease] for a given [repository].
     */
    constructor(repository: Repository) {
        repo = repository
        config = repo.config
        sv = { m -> semver(prefix(config, m)) }
        currentVersion = { m -> repo.headVersionTag(prefix(config, m))?.let { sv(m)(it) } }
        latestVersion = { m -> repo.latestVersionTag(prefix(config, m))?.let { sv(m)(it) } }
    }

    override fun close() {
        repo.close()
    }

    /**
     * Returns the [version] IF it's moreThan [latestVersion] AND not already present in the [repo],
     * ELSE returns `null`.
     *
     * Will return the version for a given [submodule] path, if specified,
     * and a matching submodule path is found in [Configuration.monorepo] config
     *
     * @param submodule optional monorepo submodule path declared in this [config] via [ModuleConfig.path]
     */
    @JvmOverloads
    fun release(version: Semver, submodule: String? = null): Semver? {
        val moduleConfig = config.monorepo.modules.firstOrNull { it.path == submodule }
        val moreThanLatest = latestVersion(moduleConfig)?.let { version > it } ?: true
        val exists by lazy {
            repo.tags(prefix(config, moduleConfig))
                .map { sv(moduleConfig)(it) }
                .any { it == version }
        }
        return if (moreThanLatest && !exists) version else null
    }

    /**
     * Returns the next release version after the [latestVersion] based on the [increment].
     *
     * See also [SemverRelease.increment] for the rules that are used when releasing the versions.
     *
     * Will return the version for a given [submodule] path, if specified,
     * and a matching submodule path is found in [Configuration.monorepo] config
     *
     * @param submodule optional monorepo submodule path declared in this [config] via [ModuleConfig.path]
     */
    @JvmOverloads
    fun release(increment: Increment, submodule: String? = null): Semver = increment(increment, true, submodule)

    /**
     * Returns the next snapshot version after the [latestVersion] based on the [increment].
     *
     * See also [SemverRelease.increment] for the rules that are used when creating version snapshots.
     *
     * Will return the version for a given [submodule] path, if specified,
     * and a matching submodule path is found in [Configuration.monorepo] config
     *
     * @param submodule optional monorepo submodule path declared in this [config] via [ModuleConfig.path]
     */
    @JvmOverloads
    fun snapshot(increment: Increment, submodule: String? = null): Semver = increment(increment, false, submodule)

    /**
     * Increment the [latestVersion] to the next [Semver] using the [increment].
     *
     * Will increment the version for a given [submodule] path, if specified,
     * and a matching submodule path is found in [Configuration.monorepo] config.
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
     * @param submodule optional monorepo submodule path declared in this [config] via [ModuleConfig.path]
     */
    @JvmOverloads
    fun increment(
        increment: Increment,
        release: Boolean = (increment != Increment.DEFAULT),
        submodule: String? = null,
    ): Semver {
        val moduleConfig = config.monorepo.modules.firstOrNull { it.path == submodule }
        val nextVersion = latestVersion(moduleConfig)?.let {
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
                    it.preRelease?.let { _ -> increment(Increment.PRE_RELEASE, release, submodule) }
                        ?: increment(config.version.defaultIncrement, release, submodule)
                }
                Increment.NONE -> latestVersion(moduleConfig)
            }
        } ?: config.version.initialVersion
        return if (release || increment == Increment.PRE_RELEASE) nextVersion else nextVersion.withSnapshot()
    }

    /**
     * Creates a pre-release version with a given [increment], and returns as [Semver] instance.
     *
     * Will create a pre-release version for a given [submodule] path, if specified,
     * and a matching submodule path is found in [Configuration.monorepo] config.
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
     *
     * @param submodule optional monorepo submodule path declared in this [config] via [ModuleConfig.path]
     */
    @JvmOverloads
    fun createPreRelease(increment: Increment, submodule: String? = null): Semver {
        val preRelease = PreRelease("${config.version.preReleaseId}.${config.version.initialPreRelease}")
        val moduleConfig = config.monorepo.modules.firstOrNull { it.path == submodule }
        return with(latestVersion(moduleConfig)) {
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

    private fun Semver.isSnapshot(): Boolean {
        return toString().endsWith("-${config.version.snapshotSuffix}")
    }

    /**
     * Promotes a pre-release version to a release version.
     *
     * Will promote a pre-release version for a given [submodule] path, if specified,
     * and a matching submodule path is found in [Configuration.monorepo] config.
     *
     * IF [latestVersion] is a pre-release version,
     * THEN a copy of the [latestVersion] is returned as a normal version (with the pre-release component stripped),
     * ELSE the [latestVersion] version is returned.
     *
     * @param submodule optional monorepo submodule path declared in this [config] via [ModuleConfig.path]
     */
    @JvmOverloads
    fun promoteToRelease(submodule: String? = null): Semver? {
        val moduleConfig = config.monorepo.modules.firstOrNull { it.path == submodule }
        return with(latestVersion(moduleConfig)) {
            this?.preRelease?.let { copy(preRelease = null, buildMetadata = null) } ?: this
        }
    }

    /**
     * Returns the next [Increment] based on the [Repository.latestVersionTag].
     *
     * Will return the next [Increment] for a given [submodule] path, if specified,
     * and a matching submodule path is found in [Configuration.monorepo] config.
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
     *
     * @param submodule optional monorepo submodule path declared in this [config] via [ModuleConfig.path]
     */
    @JvmOverloads
    fun nextIncrement(submodule: String? = null): Increment {
        val moduleConfig = config.monorepo.modules.firstOrNull { it.path == submodule }
        val prefix = prefix(config, moduleConfig)
        val head = repo.head()
        val latestTag = repo.latestVersionTag(prefix)
        val tagObjectId = latestTag?.peeledObjectId ?: latestTag?.objectId
        val isHeadOnTag by lazy { repo.tags(prefix).any { head == it.peeledObjectId || head == it.objectId } }
        return when {
            head == tagObjectId || isHeadOnTag -> Increment.NONE
            else -> repo.log(untilTag = latestTag, tagPrefix = prefix).nextIncrement()
        }
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
