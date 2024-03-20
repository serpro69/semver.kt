package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import java.nio.file.Path

/**
 * Provides access to default [Configuration] properties with optional overrides through DSL syntax
 *
 * NB! Keep in mind that the ordering of dsl functions calls matters.
 *
 * If [git] is called before [monorepo] and the latter configures [PojoGitConfig.tag] via `git { tag {} }` function,
 * all non-overwritten properties of [PojoGitTagConfig] will have the same values as [git] `tag` configuration.
 *
 * For example:
 *
 * ```kotlin
 * git {
 *     tag {
 *         prefix = "p"
 *         separator = "sep"
 *     }
 * }
 * monorepo {
 *     module("foo") {
 *         tag {
 *             prefix = "foo-p"
 *         }
 *     }
 * }
 * ```
 *
 * The "foo" module will have the following configuration:
 * ```json
 * { "name": "foo", "sources": ".", "tag": { "prefix": "foo-p", "separator": "sep", "useBranches": "false" } }
 * ```
 *
 * However, if `git` config is applied after `monorepo`,
 * then `monorepo.tag` configuration will contain the [GitTagConfig] defaults for any non-overwritten configs:
 *
 * ```kotlin
 * monorepo {
 *     module("foo") {
 *         tag {
 *             prefix = "foo-p"
 *         }
 *     }
 * }
 * git {
 *     tag {
 *         prefix = "p"
 *         separator = "sep"
 *     }
 * }
 * ```
 *
 * In this case, the "foo" module will have the following configuration:
 * ```json
 * { "name": "foo", "sources": ".", "tag": { "prefix": "foo-p", "separator": "", "useBranches": "false" } }
 * ```
 *
 * _Note the `tag.separator` is empty string, which is the default in [GitTagConfig]`_
 */
@PojoConfigDsl
class DslConfiguration internal constructor() : Configuration {

    override var git = PojoGitConfig()
        private set
    override var version = PojoVersionConfig()
        private set
    override var monorepo = PojoMonorepoConfig(git.tag)
        private set

    fun git(block: PojoGitConfig.() -> Unit): Configuration {
        git.apply(block)
        return this
    }

    fun version(block: PojoVersionConfig.() -> Unit): Configuration {
        version.apply(block)
        return this
    }

    fun monorepo(block: PojoMonorepoConfig.() -> Unit): Configuration {
        monorepo.apply(block)
        return this
    }
}

@PojoConfigDsl
class PojoMonorepoConfig internal constructor(private val tag: PojoGitTagConfig) : MonorepoConfig {
    override var sources: Path = super.sources
    override val modules: MutableList<ModuleConfig> = mutableListOf()

    fun module(path: String, block: DslModuleConfig.() -> Unit) {
        // use a copy of the tag config so that we don't overwrite "git.tag" configuration with the module's specifics
        modules.add(DslModuleConfig(path, tag.copy()).apply(block))
    }
}

@PojoConfigDsl
class DslModuleConfig internal constructor(
    override val path: String,
    private val gitTag: PojoGitTagConfig,
) : ModuleConfig {
    override var sources: Path = super.sources

    // use null as default value like in super, no need to return default configs for this prop if they haven't changed
    // but make it modifiable via dsl and a "secondary" property in constructor
    override var tag: PojoGitTagConfig? = null
        private set

    init {
        if (path.isBlank()) {
            throw IllegalArgumentException("Module path cannot be blank")
        }
    }

    /**
     * Applies the [block] function to the [tag] of this [ModuleConfig] instance
     */
    fun tag(block: PojoGitTagConfig.() -> Unit) {
        // apply configuration to "secondary" property
        gitTag.apply(block)
        // then modify tag config, so it's applied only when calling the dsl function for it
        tag = gitTag
    }
}

@Suppress("unused")
@PojoConfigDsl
class PojoVersionConfig internal constructor() : VersionConfig {
    override var initialVersion: Semver = super.initialVersion
    override var placeholderVersion: Semver = super.placeholderVersion
    override var defaultIncrement: Increment = super.defaultIncrement
    override var preReleaseId: String = super.preReleaseId
    override var initialPreRelease: Int = super.initialPreRelease
    override var snapshotSuffix: String = super.snapshotSuffix
    var useSnapshots: Boolean = false
}

@PojoConfigDsl
class PojoGitConfig internal constructor() : GitConfig {
    override val repo = PojoGitRepoConfig()
    override val tag = PojoGitTagConfig()
    override val message = PojoGitMessageConfig()

    fun repo(block: PojoGitRepoConfig.() -> Unit) {
        repo.apply(block)
    }

    fun tag(block: PojoGitTagConfig.() -> Unit) {
        tag.apply(block)
    }

    fun message(block: PojoGitMessageConfig.() -> Unit) {
        message.apply(block)
    }
}

@PojoConfigDsl
class PojoGitRepoConfig internal constructor() : GitRepoConfig {

    override var directory: Path = super.directory
    override var remoteName: String = super.remoteName
    override var cleanRule: CleanRule = super.cleanRule
}

@PojoConfigDsl
class PojoGitTagConfig internal constructor() : GitTagConfig {

    override var prefix: TagPrefix = super.prefix
    override var separator: String = super.separator
    override var useBranches: Boolean = super.useBranches

    internal fun copy(): PojoGitTagConfig = PojoGitTagConfig().apply {
        this@apply.prefix = this@PojoGitTagConfig.prefix
        this@apply.separator = this@PojoGitTagConfig.separator
        this@apply.useBranches = this@PojoGitTagConfig.useBranches
    }
}

@PojoConfigDsl
class PojoGitMessageConfig internal constructor() : GitMessageConfig {

    override var major: String = super.major
    override var minor: String = super.minor
    override var patch: String = super.patch
    override var preRelease: String = super.preRelease
    override var ignoreCase: Boolean = super.ignoreCase
}

@DslMarker
annotation class PojoConfigDsl

@PojoConfigDsl
fun DslConfiguration(block: DslConfiguration.() -> Unit): DslConfiguration {
    return DslConfiguration().apply(block)
}
