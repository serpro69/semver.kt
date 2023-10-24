package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.tasks.TagTask
import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.release.Increment.NONE
import io.github.serpro69.semverkt.release.SemverRelease
import io.github.serpro69.semverkt.release.configuration.JsonConfiguration
import io.github.serpro69.semverkt.spec.Semver
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging

@Suppress("unused")
class SemverKtPlugin : Plugin<Settings> {
    private val logger = Logging.getLogger(this::class.java)

    override fun apply(settings: Settings) {
        val config: SemverKtPluginConfig = settings.settingsDir.resolve("semantic-versioning.json").let {
            if (it.exists()) {
                logger.log(LogLevel.DEBUG, "Using semantic-versioning.json for plugin configuration...")
                return@let SemverKtPluginConfig(JsonConfiguration(it), settings)
            }
            SemverKtPluginConfig(settings)
        }
        // override configuration via settings extension
        settings.extensions.create("semantic-versioning", SemverPluginExtension::class.java, config)

        logger.log(LogLevel.DEBUG, "Using configuration: ${config.jsonString()}")

        // configure all projects with semver
        settings.gradle.allprojects { project ->
            configureProject(project, config)
        }

        logger.log(LogLevel.INFO, "Finish applying plugin...")
    }

    private fun configureProject(project: Project, config: SemverKtPluginConfig) {
        val (currentVersion, latestVersion, nextVersion) = setVersion(project, config)

        project.tasks.register("tag", TagTask::class.java) {
            it.description = "Create a tag for the next version"
            it.config.set(config)
            it.dryRun.set(project.hasProperty("dryRun"))
            // set versions
            it.currentVersion.set(currentVersion)
            it.latestVersion.set(latestVersion)
            it.nextVersion.set(nextVersion)
        }
    }

    private fun setVersion(project: Project, config: SemverKtPluginConfig): Triple<Semver?, Semver?, Semver?> {
        val propPromoteRelease = project.hasProperty("promoteRelease")
        val propPreRelease = project.hasProperty("preRelease")
        val propIncrement = project.findProperty("increment")
            ?.let { Increment.getByName(it.toString()) }
            ?: NONE

        return with(SemverRelease(config)) {
            // IF git HEAD points at a version, set and return it and don't do anything else
            currentVersion()?.let {
                project.version = it
                return@with Triple(it, null, null)
            }
            // ELSE figure out next version
            val latestVersion = latestVersion()
            logger.log(LogLevel.INFO, "Current version: $latestVersion")
            val increaseVersion = if (propPromoteRelease) NONE else with(nextIncrement()) {
                logger.log(LogLevel.DEBUG, "Next increment from property: $propIncrement")
                logger.log(LogLevel.DEBUG, "Next increment from git commit: $this")
                when (propIncrement) {
                    // 1. check allowed values for gradle-property based increment
                    // IF nextIncrement is NONE - we don't want to override it
                    //  -> it contains some logic for checking existing versions and HEAD version
                    // ELSE return increment from property
                    // 2. just return the result of nextIncrement
                    //  -> DEFAULT and NONE are not valid for property-based increment
                    !in listOf(
                        Increment.DEFAULT,
                        NONE
                    ) -> if (this == NONE) this else propIncrement
                    else -> this
                }
            }
            logger.log(LogLevel.INFO, "Calculated version increment: '$increaseVersion'")
            val nextVersion = if (propPromoteRelease) {
                logger.log(LogLevel.INFO, "Promote to release...")
                promoteToRelease()
            } else if (propPreRelease) {
                logger.log(LogLevel.INFO, "Create pre-release...")
                createPreRelease(increaseVersion)
            } else when (increaseVersion) {
                Increment.MAJOR, Increment.MINOR, Increment.PATCH -> {
                    logger.log(LogLevel.INFO, "Create release...")
                    release(increaseVersion)
                }
                Increment.PRE_RELEASE -> {
                    latestVersion?.preRelease?.let {
                        logger.log(LogLevel.INFO, "Next pre-release...")
                        release(increaseVersion)
                    } ?: run {
                        logger.log(LogLevel.INFO, "Create default pre-release...")
                        createPreRelease(Increment.DEFAULT)
                    }
                }
                Increment.DEFAULT, NONE -> null
            }
            logger.log(LogLevel.INFO, "Next version: $nextVersion")
            when {
                (nextVersion != null && latestVersion != null) && (nextVersion >= latestVersion) -> {
                    // only set version if nextVersion >= latestVersion
                    project.version = nextVersion
                    logger.log(LogLevel.DEBUG, "Set project.version: ${(project.version)}")
                }
                nextVersion != null && latestVersion == null -> {
                    // set initial version
                    project.version = nextVersion
                    logger.log(LogLevel.DEBUG, "Set project.version: ${(project.version)}")
                }
                else -> {
//                    if (config.useSnapshots) {
//                        project.version = snapshot(increaseVersion)
//                        logger.log(LogLevel.DEBUG, "Set project.version: ${(project.version)}")
//                    }
                    logger.log(LogLevel.DEBUG, "Not doing anything...")
                }
            }
            logger.log(LogLevel.INFO, "Done...")
            Triple(null, latestVersion, nextVersion)
        }
    }
}
