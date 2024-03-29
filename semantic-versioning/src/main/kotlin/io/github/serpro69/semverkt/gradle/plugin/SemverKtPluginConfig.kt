package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.release.configuration.CleanRule
import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitConfig
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitRepoConfig
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.configuration.ModuleConfig
import io.github.serpro69.semverkt.release.configuration.MonorepoConfig
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.release.configuration.VersionConfig
import io.github.serpro69.semverkt.spec.Semver
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import java.nio.file.Path

private val logger = Logging.getLogger("SemverKtPluginConfig")

@PluginConfigDsl
class SemverKtPluginConfig(settings: Settings?) : Configuration {

    override val git = SemverKtPluginGitConfig(settings)
    override val version = SemverKtPluginVersionConfig()
    override val monorepo = SemverKtPluginMonorepoConfig(settings, git.tag)

    constructor(config: Configuration, settings: Settings? = null) : this(settings) {
        git {
            repo {
                directory = settings?.settingsDir?.toPath() ?: config.git.repo.directory
                remoteName = config.git.repo.remoteName
                cleanRule = config.git.repo.cleanRule
            }
            message {
                major = config.git.message.major
                minor = config.git.message.minor
                patch = config.git.message.patch
                preRelease = config.git.message.preRelease
                ignoreCase = config.git.message.ignoreCase
            }
            tag {
                prefix = config.git.tag.prefix
                separator = config.git.tag.separator
                useBranches = config.git.tag.useBranches
            }
        }
        version {
            initialVersion = config.version.initialVersion
            placeholderVersion = config.version.placeholderVersion
            defaultIncrement = config.version.defaultIncrement
            preReleaseId = config.version.preReleaseId
            initialPreRelease = config.version.initialPreRelease
            snapshotSuffix = config.version.snapshotSuffix
            useSnapshots = false
        }
        monorepo {
            sources = settings?.settingsDir?.toPath() ?: config.git.repo.directory
            modules.addAll(config.monorepo.modules)
        }
    }

    fun git(block: SemverKtPluginGitConfig.() -> Unit): Configuration {
        git.apply(block)
        return this
    }

    fun version(block: SemverKtPluginVersionConfig.() -> Unit): Configuration {
        version.apply(block)
        return this
    }

    fun monorepo(block: SemverKtPluginMonorepoConfig.() -> Unit): Configuration {
        monorepo.apply(block)
        return this
    }
}

@PluginConfigDsl
class SemverKtPluginVersionConfig internal constructor() : VersionConfig {
    override var initialVersion: Semver = super.initialVersion
    override var placeholderVersion: Semver = super.placeholderVersion
    override var defaultIncrement: Increment = super.defaultIncrement
    override var preReleaseId: String = super.preReleaseId
    override var initialPreRelease: Int = super.initialPreRelease
    override var snapshotSuffix: String = super.snapshotSuffix
    var useSnapshots: Boolean = false
}

@PluginConfigDsl
class SemverKtPluginGitConfig internal constructor(settings: Settings?) : GitConfig {
    override val repo = SemverKtPluginGitRepoConfig(settings)
    override val tag = SemverKtPluginGitTagConfig()
    override val message = SemverKtPluginGitMessageConfig()

    fun repo(block: SemverKtPluginGitRepoConfig.() -> Unit) {
        repo.apply(block)
    }

    fun tag(block: SemverKtPluginGitTagConfig.() -> Unit) {
        tag.apply(block)
    }

    fun message(block: SemverKtPluginGitMessageConfig.() -> Unit) {
        message.apply(block)
    }
}

@PluginConfigDsl
class SemverKtPluginGitRepoConfig internal constructor(settings: Settings?) : GitRepoConfig {

    // use settings dir as default path for plugin config
    override var directory: Path = settings?.settingsDir?.toPath() ?: super.directory
    override var remoteName: String = super.remoteName
    override var cleanRule: CleanRule = super.cleanRule
}

@PluginConfigDsl
class SemverKtPluginGitTagConfig internal constructor() : GitTagConfig {

    override var prefix: TagPrefix = super.prefix
    override var separator: String = super.separator
    override var useBranches: Boolean = super.useBranches

    internal fun copy() = SemverKtPluginGitTagConfig().apply {
        this@apply.prefix = this@SemverKtPluginGitTagConfig.prefix
        this@apply.separator = this@SemverKtPluginGitTagConfig.separator
        this@apply.useBranches = this@SemverKtPluginGitTagConfig.useBranches
    }
}

@PluginConfigDsl
class SemverKtPluginGitMessageConfig internal constructor() : GitMessageConfig {

    override var major: String = super.major
    override var minor: String = super.minor
    override var patch: String = super.patch
    override var preRelease: String = super.preRelease
    override var ignoreCase: Boolean = super.ignoreCase
}

@PluginConfigDsl
class SemverKtPluginMonorepoConfig internal constructor(settings: Settings?, private val tag: SemverKtPluginGitTagConfig) : MonorepoConfig {
    override var sources: Path = settings?.settingsDir?.toPath() ?: super.sources
    override val modules: MutableList<ModuleConfig> = mutableListOf()

    fun module(name: String, block: SemverKtPluginModuleConfig.() -> Unit) {
        logger.debug("Configure module {}", name)
        // use a copy of the tag config so that we don't overwrite "git.tag" configuration with the module's specifics
        modules.add(SemverKtPluginModuleConfig(name, tag.copy()).apply(block))
        logger.debug("Project modules config:\n {}", modules.joinToString("\n ") { it.jsonString() })
    }
}

/**
 * @property path a fully-qualified gradle module path.
 * For example, for `./core` module in the root of a gradle mono-repo, this would be `:core`,
 * and for `./foo/bar` module in a gradle mono-repo, this would be `:foo:bar`.
 */
@PluginConfigDsl
class SemverKtPluginModuleConfig internal constructor(
    override val path: String,
    private val gitTag: SemverKtPluginGitTagConfig,
) : ModuleConfig {
    override var sources: Path = super.sources

    // use super as default, no need to return default configs for this prop if they haven't changed
    // but make it modifiable via dsl and a "secondary" property in constructor
    override var tag: GitTagConfig? = super.tag
        private set

    init {
        if (path.isBlank()) {
            throw IllegalArgumentException("Module name cannot be blank")
        }
    }

    /**
     * Applies the [block] function to the [tag] of this [SemverKtPluginModuleConfig] instance
     */
    fun tag(block: SemverKtPluginGitTagConfig.() -> Unit) {
        // apply configuration to "secondary" property
        gitTag.apply(block)
        // then modify tag config, so it's applied only when calling the dsl function for it
        tag = gitTag
    }
}

@DslMarker
annotation class PluginConfigDsl
