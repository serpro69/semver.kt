package io.github.serpro69.semverkt.gradle.plugin.tasks

import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.gradle.api.logging.LogLevel.DEBUG
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class TagTask : SemverReleaseTask() {
    private val logger = Logging.getLogger(this::class.java)

    @get:Internal
    internal abstract val dryRun: Property<Boolean>

    @TaskAction
    fun tag() {
        val (current, latest, next) = Triple(currentVersion.orNull, latestVersion.orNull, nextVersion.orNull)
        logger.log(DEBUG, "latestVersion: $latest")
        logger.log(DEBUG, "nextVersion: $next")
        when {
            // current version points at HEAD - don't do anything
            current != null -> logger.log(LIFECYCLE, "Current version: $current")
            // release new version
            (next != null && latest != null) && (next > latest) -> {
                logger.log(LIFECYCLE, "Calculated next version: $next")
                setTag(next)
            }
            // release first version
            next != null && latest == null -> {
                logger.log(LIFECYCLE, "Calculated next version: $next")
                setTag(next)
            }
            else -> logger.log(LIFECYCLE, "Not doing anything")
        }
    }

    private fun setTag(nextVer: Semver) {
        if (!dryRun.get()) with(project.buildFile.parentFile) {
            logger.log(DEBUG, "Open repo at: $this")
            val repo = Git.open(this)
            logger.log(DEBUG, "Set tag to: v$nextVer")
            repo.setTag("v$nextVer")
        }
    }
}

private fun Git.setTag(tagName: String) {
    Git(repository).use { git ->
        git.tag().setName(tagName).setMessage(tagName).setAnnotated(true).call()
    }
}
