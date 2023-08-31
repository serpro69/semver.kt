package io.github.serpro69.semverkt.gradle.plugin

@Suppress("unused")
@ConfigDsl
abstract class SemverPluginExtension(val config: SemverKtPluginConfig) {

    fun git(block: SemverKtPluginGitConfig.() -> Unit): SemverPluginExtension {
        config.git.apply(block)
        return this
    }

    fun version(block: SemverKtPluginVersionConfig.() -> Unit): SemverPluginExtension {
        config.version.apply(block)
        return this
    }
}
