package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.JsonConfiguration
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging

class SemverKtPlugin : Plugin<Settings> {
    private val logger = Logging.getLogger(this::class.java)

    override fun apply(settings: Settings) {
        val config: Configuration = settings.settingsDir.resolve("semver-release.json").let {
            if (!it.exists()) {
                logger.log(LogLevel.DEBUG, "semver-release.json file not found in settings dir")
                return@let SemverKtPluginConfig()
            }
            JsonConfiguration(it)
        }
        settings.extensions.create("semver-release", SemverPluginExtension::class.java, config)
        settings.gradle.allprojects { project ->
//            project.version = Semver("1.2.3")
//            logger.log(LogLevel.LIFECYCLE, (project.version as Semver).normalVersion)

            // override configuration via extension
            project.extensions.create("semver-release", SemverPluginExtension::class.java, config)
        }

        logger.log(LogLevel.LIFECYCLE, config.jsonString())
        logger.log(LogLevel.LIFECYCLE, "Finish applying plugin...")
    }
}
