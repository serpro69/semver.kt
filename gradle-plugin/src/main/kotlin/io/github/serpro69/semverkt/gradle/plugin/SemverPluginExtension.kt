package io.github.serpro69.semverkt.gradle.plugin

@ConfigDsl
abstract class SemverPluginExtension(private val config: SemverKtPluginConfig) {

    fun git(block: SemverKtPluginGitConfig.() -> Unit): SemverPluginExtension {
        config.git.apply(block)
        return this
    }

    fun version(block: SemverKtPluginVersionConfig.() -> Unit): SemverPluginExtension {
        config.version.apply(block)
        return this
    }
}
