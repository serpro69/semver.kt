package io.github.serpro69.semverkt.release.configuration

import dev.nohus.autokonfig.AutoKonfig
import dev.nohus.autokonfig.withProperties
import java.nio.file.Path
import java.util.*

class PropertiesConfiguration(properties: Properties = Properties()) : ConfigurationProvider {
    private val autoConfig: AutoKonfig = AutoKonfig().withProperties(properties)

    private val gitRepoConfig = object : GitRepoConfig {
        override val directory: Path = autoConfig.propertyOrNull("git.repo.directory") ?: super.directory
        override val remoteName: String = autoConfig.propertyOrNull("git.repo.remoteName") ?: super.remoteName
    }

    private val gitTagConfig = object : GitTagConfig {
        override val prefix: String = autoConfig.propertyOrNull("git.tag.prefix") ?: super.prefix
        override val separator: String = autoConfig.propertyOrNull("git.tag.separator") ?: super.separator
        override val useBranches: Boolean = autoConfig.propertyOrNull("git.tag.useBranches") ?: super.useBranches
    }

    private val gitMessageConfig = object : GitMessageConfig {
        override val major: String = autoConfig.propertyOrNull("git.message.major") ?: super.major
        override val minor: String = autoConfig.propertyOrNull("git.message.minor") ?: super.minor
        override val patch: String = autoConfig.propertyOrNull("git.message.patch") ?: super.patch
        override val preRelease: String = autoConfig.propertyOrNull("git.message.preRelease") ?: super.preRelease
        override val ignoreCase: Boolean = autoConfig.propertyOrNull("git.message.ignoreCase") ?: super.ignoreCase
    }

    override val git: GitConfig = object : GitConfig {
        override val repo: GitRepoConfig = gitRepoConfig
        override val tag: GitTagConfig = gitTagConfig
        override val message: GitMessageConfig = gitMessageConfig
    }

    override val version: VersionConfig = object : VersionConfig {
        override val preReleaseId: String = autoConfig.propertyOrNull("version.preReleaseId") ?: super.preReleaseId
    }
}
