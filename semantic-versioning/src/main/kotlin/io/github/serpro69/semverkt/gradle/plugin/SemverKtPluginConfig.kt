package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitConfig
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitRepoConfig
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.configuration.VersionConfig
import io.github.serpro69.semverkt.spec.Semver
import org.gradle.api.initialization.Settings
import java.nio.file.Path

@ConfigDsl
class SemverKtPluginConfig(settings: Settings?) : Configuration {

    constructor(config: Configuration, settings: Settings? = null) : this(settings) {
        git {
            repo {
                directory = settings?.settingsDir?.toPath() ?: config.git.repo.directory
                remoteName = config.git.repo.remoteName
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
        }
    }

    override var git = SemverKtPluginGitConfig(settings)
        private set
    override var version = SemverKtPluginVersionConfig()
        private set

    fun git(block: SemverKtPluginGitConfig.() -> Unit): Configuration {
        git.apply(block)
        return this
    }

    fun version(block: SemverKtPluginVersionConfig.() -> Unit): Configuration {
        version.apply(block)
        return this
    }
}

@ConfigDsl
class SemverKtPluginVersionConfig internal constructor() : VersionConfig {
    override var initialVersion: Semver = super.initialVersion
    override var placeholderVersion: Semver = super.placeholderVersion
    override var defaultIncrement: Increment = super.defaultIncrement
    override var preReleaseId: String = super.preReleaseId
    override var initialPreRelease: Int = super.initialPreRelease
    override var snapshotSuffix: String = super.snapshotSuffix
}

@ConfigDsl
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

@ConfigDsl
class SemverKtPluginGitRepoConfig internal constructor(settings: Settings?) : GitRepoConfig {

    override var directory: Path = settings?.settingsDir?.toPath() ?: super.directory // use settings dir as default path for plugin config
    override var remoteName: String = super.remoteName
}

@ConfigDsl
class SemverKtPluginGitTagConfig internal constructor() : GitTagConfig {

    override var prefix: String = super.prefix
    override var separator: String = super.separator
    override var useBranches: Boolean = super.useBranches
}

@ConfigDsl
class SemverKtPluginGitMessageConfig internal constructor() : GitMessageConfig {

    override var major: String = super.major
    override var minor: String = super.minor
    override var patch: String = super.patch
    override var preRelease: String = super.preRelease
    override var ignoreCase: Boolean = super.ignoreCase
}

@DslMarker
annotation class ConfigDsl
