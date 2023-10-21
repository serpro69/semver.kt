package io.github.serpro69.semverkt.gradle.plugin

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging

@ConfigDsl
abstract class SemverPluginExtension(private val config: SemverKtPluginConfig) {
    private val logger = Logging.getLogger(this::class.java)

    fun git(block: SemverKtPluginGitConfig.() -> Unit): SemverPluginExtension {
        logger.log(LogLevel.DEBUG, "Apply git config via extension")
        config.git.apply(block)
        return this
    }

    fun version(block: SemverKtPluginVersionConfig.() -> Unit): SemverPluginExtension {
        logger.log(LogLevel.DEBUG, "Apply version config via extension")
        config.version.apply(block)
        return this
    }
}
