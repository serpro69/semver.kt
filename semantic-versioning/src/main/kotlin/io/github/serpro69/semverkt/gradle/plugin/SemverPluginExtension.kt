package io.github.serpro69.semverkt.gradle.plugin

import org.gradle.api.logging.Logging

@Suppress("unused")
@PluginConfigDsl
abstract class SemverPluginExtension(private val config: SemverKtPluginConfig) {
    private val logger = Logging.getLogger(this::class.java)

    fun git(block: SemverKtPluginGitConfig.() -> Unit): SemverPluginExtension {
        logger.debug("Apply git config via extension")
        config.git.apply(block)
        return this
    }

    fun version(block: SemverKtPluginVersionConfig.() -> Unit): SemverPluginExtension {
        logger.debug("Apply version config via extension")
        config.version.apply(block)
        return this
    }

    fun monorepo(block: SemverKtPluginMonorepoConfig.() -> Unit): SemverPluginExtension {
        logger.debug("Apply monorepo config via extension")
        config.monorepo.apply(block)
        return this
    }
}
