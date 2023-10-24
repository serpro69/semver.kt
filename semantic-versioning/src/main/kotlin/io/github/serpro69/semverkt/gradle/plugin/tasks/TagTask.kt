package io.github.serpro69.semverkt.gradle.plugin.tasks

import io.github.serpro69.semverkt.gradle.plugin.SemverKtPluginConfig
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.logging.LogLevel.DEBUG
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class TagTask : SemverReleaseTask() {
    private val logger = Logging.getLogger(this::class.java)

    @get:Internal
    internal abstract val dryRun: Property<Boolean>

    @get:Internal
    internal abstract val config: Property<SemverKtPluginConfig>

    init {
        // check if tag already set, if so tell plugin it's UP-TO-DATE
        // NB! it won't handle multi-module projects because currentVersion will be set to all projects
        // before the task itself is even evaluated and executed
        outputs.upToDateWhen { currentVersion.orNull != null }
        //this@TagTask.onlyIf { currentVersion.orNull == null || nextVersion.orNull != null }
    }

    @TaskAction
    fun tag() {
        // check if tag already set, if so tell plugin it's UP-TO-DATE
        val (current, latest, next) = Triple(currentVersion.orNull, latestVersion.orNull, nextVersion.orNull)
        logger.log(DEBUG, "latestVersion: $latest")
        logger.log(DEBUG, "nextVersion: $next")
        when {
            // current version points at HEAD - don't do anything
            current != null -> logger.log(LIFECYCLE, "Current version: $current")
            // release new version
            (next != null && latest != null) && (next > latest) -> {
                logger.log(LIFECYCLE, "Calculated next version: $next")
                setTag(next, config.get())
            }
            // release first version
            next != null && latest == null -> {
                logger.log(LIFECYCLE, "Calculated next version: $next")
                setTag(next, config.get())
            }
            else -> logger.log(LIFECYCLE, "Not doing anything")
        }
    }

    private fun setTag(nextVer: Semver, config: SemverKtPluginConfig) {
        if (!dryRun.get()) run {
            // TODO is there a better way to handle multi-module projects?
            //  tag might already exist (was set by root project or another sub-module)
            val isHeadOnTag = GitRepository(config).use { repo ->
                repo.tags().let { tags ->
                    tags.isNotEmpty()
                        && tags.last().let { t -> repo.head() == t.peeledObjectId || repo.head() == t.objectId }
                }
            }
            if (!isHeadOnTag) {
                logger.log(DEBUG, "Open repo at: $this")
                FileRepositoryBuilder()
                    .setWorkTree(project.projectDir)
                    .findGitDir(project.projectDir)
                    .build()
                    .use {
                        logger.log(DEBUG, "Set tag to: v$nextVer")
                        Git(it).use { git -> git.setTag("v$nextVer") }
                    }
            } else logger.log(INFO, "Tag v$nextVer already exists in project")
        }
    }
}

private fun Git.setTag(tagName: String) {
    tag().setName(tagName).setMessage(tagName).setAnnotated(true).call()
}
