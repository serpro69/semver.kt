package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitConfig
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitRepoConfig
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.configuration.VersionConfig
import io.github.serpro69.semverkt.spec.Semver
import java.nio.file.Path

@ConfigDsl
class SemverKtPluginConfig : Configuration {

    override var git = SemverKtPluginGitConfig()
        private set
    override var version = SemverKtPluginVersionConfig()
        private set

    fun git(block: SemverKtPluginGitConfig.() -> Unit): Configuration {
        block.invoke(git)
        return this
    }

    fun version(block: SemverKtPluginVersionConfig.() -> Unit): Configuration {
        block.invoke(version)
        return this
    }
}

@ConfigDsl
class SemverKtPluginVersionConfig internal constructor() : VersionConfig {
    override var initialVersion: Semver = super.initialVersion
    override var defaultIncrement: Increment = super.defaultIncrement
    override var preReleaseId: String = super.preReleaseId
    override var initialPreRelease: Int = super.initialPreRelease
    override var snapshotSuffix: String = super.snapshotSuffix
}

@ConfigDsl
class SemverKtPluginGitConfig internal constructor() : GitConfig {
    override var repo = SemverKtPluginGitRepoConfig()
        private set
    override var tag = SemverKtPluginGitTagConfig()
        private set
    override var message = SemverKtPluginGitMessageConfig()
        private set

    fun repo(block: SemverKtPluginGitRepoConfig.() -> Unit) {
        block.invoke(repo)
    }

    fun tag(block: SemverKtPluginGitTagConfig.() -> Unit) {
        block.invoke(tag)
    }

    fun message(block: SemverKtPluginGitMessageConfig.() -> Unit) {
        block.invoke(message)
    }
}

@ConfigDsl
class SemverKtPluginGitRepoConfig internal constructor() : GitRepoConfig {

    override var directory: Path = super.directory
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
