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
import io.github.serpro69.semverkt.release.configuration.JsonConfiguration
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.spec.PreRelease
import io.github.serpro69.semverkt.spec.Semver
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import kotlin.io.path.Path

@Suppress("unused")
class SemverKtPlugin : Plugin<Settings> {
    private val logger = Logging.getLogger(this::class.java)

    override fun apply(settings: Settings) {
        val config: SemverKtPluginConfig = settings.settingsDir.resolve("semantic-versioning.json").let {
            if (it.exists()) {
                logger.debug("Using semantic-versioning.json for plugin configuration...")
                return@let SemverKtPluginConfig(JsonConfiguration(it), settings)
            }
            SemverKtPluginConfig(settings)
        }
        // override configuration via settings extension
        settings.extensions.create("semantic-versioning", SemverPluginExtension::class.java, config)

//        logger.info("Using configuration: {}", config.jsonString())

        // configure all projects with semver
        settings.gradle.allprojects { project ->
            configureProject(project, config)
        }

        logger.info("Finish applying plugin...")
    }

    private fun configureProject(project: Project, config: SemverKtPluginConfig) {
        logger.info("Configure {}", project.name)
        logger.info("Using configuration: {}", config.jsonString())
        val (currentVersion, latestVersion, nextVersion) = setVersion(project, config)
        val moduleConfig = when (project.path) {
            project.rootProject.path -> null
            else -> config.monorepo.modules.firstOrNull { m -> m.path == project.path }
        }
        logger.info("{} project module config: {}", project.name,  moduleConfig?.jsonString())
        project.tasks.register("tag", TagTask::class.java) {
            it.description = "Create a tag for the next version"
            it.config.set(config)
            it.moduleConfig.set(moduleConfig)
            it.dryRun.set(project.hasProperty("dryRun"))
            // set versions
            it.currentVersion.set(currentVersion)
            it.latestVersion.set(latestVersion)
            it.nextVersion.set(nextVersion)
        }
    }

    /**
     * Calculates and sets next version for the [project] using the given [config],
     * and returns a [Triple] of versions, where
     * - `first` is currentVersion if it exists, or `null` otherwise
     * - `second` is latestVersion if it exists, or `null` otherwise
     * - `third` is the calculated nextVersion if it exists (given the current inputs and configuration),
     *    or `null` otherwise
     *
     * IF `currentVersion` exists, both `latestVersion` and `nextVersion` will always return as `null`
     */
    private fun setVersion(project: Project, config: SemverKtPluginConfig): Triple<Semver?, Semver?, Semver?> {
        logger.info("Set version for {}", project.name)
        logger.debug("Using configuration: {}", config.jsonString())
        val propRelease = project.hasProperty("release")
        val propPromoteRelease = project.hasProperty("promoteRelease")
        val propPreRelease = project.hasProperty("preRelease")
        val propIncrement = project.findProperty("increment")
            ?.let { Increment.getByName(it.toString()) }
            ?: NONE

        val isMonorepo = config.monorepo.modules.isNotEmpty()
        val module = config.monorepo.modules.firstOrNull { it.path == project.path }
        logger.debug("Module '{}' config: {}", module?.path, module?.jsonString())
        val hasChanges by lazy {
            if (project == project.rootProject) true // always apply version to root project
            else if (isMonorepo && module != null) GitRepository(config).use { repo ->
                val prefix = module.tag?.prefix ?: config.git.tag.prefix
                val s = repo.head().name
                val e = repo.headVersionTag(prefix)?.name
                    ?: repo.latestVersionTag(prefix)?.name
                    ?: repo.log(tagPrefix = prefix).last().objectId.name
                repo.diff(s, e).any {
                    logger.debug("{} diff: {}", project.name, it)
                    val srcPath = Path(project.projectDir.relativeTo(project.rootDir).path)
                        .resolve(module.sources).normalize()
                    logger.debug("Module path: {}", srcPath)
                    Path(it.oldPath).startsWith(srcPath) || Path(it.newPath).startsWith(srcPath)
                }
            }
            else true // module not versioned separately
        }
        logger.info("Module has changes: {}", hasChanges)

        return SemverRelease(config).use { svr ->
            // IF git HEAD points at a version, set and return it and don't do anything else
            svr.currentVersion(module)?.let {
                project.version = it
                return@use Triple(it, null, null)
            }
            // ELSE IF version is specified via gradle property, use that as nextVersion
            // NB! this explicitly applies to all monorepo modules regardless of whether a module had changes or not
            val v = project.findProperty("version")?.toString()
            if (v != null // TODO is this needed? isn't it always true in gradle?
                && v != config.version.placeholderVersion.toString()
                && v != "unspecified"
                && Semver.isValid(v)
            ) {
                project.version = Semver(v)
                return@use Triple(null, null, Semver(v))
            }
            // ELSE figure out next version
            val latestVersion = svr.latestVersion(module)
            logger.info("Latest version: {}", latestVersion)
            val increaseVersion = if (propPromoteRelease || !hasChanges) NONE else with(svr.nextIncrement(module?.path)) {
                logger.debug("Next increment from property: {}", propIncrement)
                logger.debug("Next increment from git commit: {}", this)
                when (propIncrement) {
                    // 1. check allowed values for gradle-property based increment
                    //    - DEFAULT is not used with 'increment' property
                    //    - NONE means 'increment' property is not set or has invalid value
                    // IF nextIncrement is NONE
                    //    - we don't want to override it because contains some logic
                    //      for checking existing versions and HEAD version
                    // OR release property is not set
                    //    - releasing from command line requires a 'release' property to be used
                    // THEN return nextIncrement
                    // ELSE return increment from property
                    // 2. just return the result of nextIncrement
                    !in listOf(DEFAULT, NONE) -> if (this == NONE || !propRelease) this else propIncrement
                    else -> this
                }
            }
            logger.info("Calculated version increment: {}", increaseVersion)
            val nextVersion = if (!hasChanges) null else if (propPromoteRelease) {
                with(svr.promoteToRelease(module?.path)) {
                    logger.info("Promote {} to {}", latestVersion, this)
                    when {
                        propRelease -> this
                        config.version.useSnapshots -> this?.copy(
                            preRelease = PreRelease(config.version.snapshotSuffix),
                            buildMetadata = null
                        )
                        else -> null
                    }
                }
            } else if (propPreRelease) {
                val inc = if (increaseVersion in listOf(DEFAULT, NONE)) DEFAULT else increaseVersion
                with(svr.createPreRelease(inc, module?.path)) {
                    logger.info("Create pre-release {} from {}", this, latestVersion)
                    when {
                        propRelease -> this
                        else -> null
                    }
                }
            } else when (increaseVersion) {
                MAJOR, MINOR, PATCH -> {
                    logger.info("Create release...")
                    svr.release(increaseVersion, module?.path)
                }
                PRE_RELEASE -> {
                    latestVersion?.preRelease?.let {
                        logger.info("Next pre-release...")
                        svr.release(increaseVersion, module?.path)
                    } ?: run {
                        logger.info("Create default pre-release...")
                        svr.createPreRelease(DEFAULT, module?.path)
                    }
                }
                DEFAULT, NONE -> when {
                    // if -Prelease is set but no -Pincrement or keyword found, then release with default increment
                    propRelease -> svr.release(config.version.defaultIncrement, module?.path)
                    // if -Prelease is NOT set then create snapshot version if snapshots are enabled in configuration
                    config.version.useSnapshots -> {
                        val inc = when (propIncrement) {
                            // DEFAULT is not used with 'increment' property
                            // NONE means 'increment' property is not set or has invalid value
                            !in listOf(DEFAULT, NONE) -> propIncrement
                            else -> config.version.defaultIncrement
                        }
                        svr.snapshot(inc, module?.path)
                    }
                    else -> null
                }
            }
            logger.info("Next version: {}", nextVersion)
            // set snapshot version explicitly because comparison with latestVersion might fail
            // depending on the snapshot suffix
            //  - because X.Y.Z-SNAPSHOT is considered a pre-release from semver perspective
            //  - and identifiers with letters or hyphens are compared lexically in ASCII sort order
            //    (see also https://semver.org/#spec-item-11 -> 4)
            //  e.g. 1.0.0-SNAPSHOT < 1.0.0-rc.1
            val setSnapshot by lazy { nextVersion.toString().endsWith(config.version.snapshotSuffix) }
            // only set next version if nextVersion >= latestVersion
            val setNext by lazy { (nextVersion != null && latestVersion != null) && (nextVersion >= latestVersion) }
            // set initial version
            val setInitial by lazy { latestVersion == null }
            if (nextVersion != null && (setSnapshot || setNext || setInitial)) {
                project.version = nextVersion
                logger.debug("Set project.version: {}", project.version)
            } else logger.debug("Not doing anything...")
            logger.info("Done...")
            Triple(null, latestVersion, nextVersion)
        }
    }
}
