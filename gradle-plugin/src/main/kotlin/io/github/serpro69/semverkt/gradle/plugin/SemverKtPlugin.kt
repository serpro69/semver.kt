package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.tasks.TagTask
import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.release.Increment.DEFAULT
import io.github.serpro69.semverkt.release.Increment.MAJOR
import io.github.serpro69.semverkt.release.Increment.MINOR
import io.github.serpro69.semverkt.release.Increment.NONE
import io.github.serpro69.semverkt.release.Increment.PATCH
import io.github.serpro69.semverkt.release.Increment.PRE_RELEASE
import io.github.serpro69.semverkt.release.SemverRelease
import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.JsonConfiguration
import io.github.serpro69.semverkt.spec.Semver
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging

@Suppress("unused")
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

            val promoteRelease = project.hasProperty("promoteRelease")
            val preRelease = project.hasProperty("preRelease")
            val increment = project.findProperty("increment")?.let { Increment.getByName(it.toString()) }
                ?: NONE

            val (latestVersion, nextVersion) = with(SemverRelease(config)) {
                val latestVersion = currentVersion()
                logger.log(LogLevel.INFO, "Current version: $latestVersion")
                val increaseVersion = if (promoteRelease) NONE else with(nextIncrement()) {
                    logger.log(LogLevel.DEBUG,"Next increment from property: $increment")
                    logger.log(LogLevel.DEBUG,"Next increment from git commit: $this")
                    if (increment !in listOf(DEFAULT, NONE)) {
                        if (this == NONE) this else increment
                    } else this
                }
                logger.log(LogLevel.INFO, "Calculated version increment: '$increaseVersion'")
                val nextVersion = if (promoteRelease) {
                    logger.log(LogLevel.INFO, "Promote to release...")
                    promoteToRelease()
                } else if (preRelease) {
                    logger.log(LogLevel.INFO, "Create pre-release...")
                    createPreRelease(increaseVersion)
                } else when (increaseVersion) {
                    MAJOR, MINOR, PATCH -> release(increaseVersion)
                    PRE_RELEASE -> {
                        latestVersion?.preRelease?.let {
                            release(increaseVersion)
                        } ?: createPreRelease(DEFAULT)
                    }
                    DEFAULT, NONE -> latestVersion
                }
                logger.log(LogLevel.LIFECYCLE, "Next version: $nextVersion")
                project.version = nextVersion as Semver
                logger.log(LogLevel.LIFECYCLE, "Set project.version: ${(project.version as Semver)}")
                logger.log(LogLevel.INFO, "Done...")
                latestVersion to nextVersion
            }

            project.tasks.register("tag", TagTask::class.java) {
                it.description = "Create a tag for the next version"
                it.latestVersion.set(latestVersion)
                it.nextVersion.set(nextVersion)
            }
        }

        logger.log(LogLevel.DEBUG, "Using configuration: ${config.jsonString()}")
        logger.log(LogLevel.DEBUG, "Finish applying plugin...")
    }
}
