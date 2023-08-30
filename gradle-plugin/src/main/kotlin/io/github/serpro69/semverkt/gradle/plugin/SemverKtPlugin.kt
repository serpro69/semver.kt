package io.github.serpro69.semverkt.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging

class SemverKtPlugin : Plugin<Settings> {
    private val logger = Logging.getLogger(this::class.java)

    override fun apply(settings: Settings) {
//        logger.log(LogLevel.ERROR, settings.rootDir.absolutePath)
        settings.gradle.allprojects { project ->
            project.version = "1.2.3"
//            logger.log(LogLevel.ERROR, project.version.toString())
        }
//        logger.log(LogLevel.ERROR, settings.gradle.rootProject.version.toString())
    }
}
