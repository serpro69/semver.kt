package io.github.serpro69.semverkt.gradle.plugin.tasks

import io.github.serpro69.semverkt.release.Increment.*
import io.github.serpro69.semverkt.release.SemverRelease
import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.gradle.api.logging.LogLevel.*
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class TagTask : SemverReleaseTask() {
    private val logger = Logging.getLogger(this::class.java)

    @get:Internal
    internal abstract val latestVersion: Property<Semver>
    @get:Internal
    internal abstract val nextVersion: Property<Semver>
    @get:Internal
    internal val dryRun = project.hasProperty("dryRun")

    @TaskAction
    fun tag() {
        val (latest, next) = latestVersion.orNull to nextVersion.orNull
        logger.log(DEBUG, "latestVersion: $latest")
        logger.log(DEBUG, "nextVersion: $next")
        if (next == latest) {
            logger.log(LIFECYCLE, "latestVersion == nextVersion, not doing anything")
        } else if (!dryRun) {
            with(project.buildFile.parentFile) {
                logger.log(DEBUG, "Open repo at: $this")
                val repo = Git.open(this)
                logger.log(DEBUG, "Set tag to: v$next")
                repo.setTag("v$next")
            }
        } else {
            logger.log(LIFECYCLE, "Calculated next version: $next")
        }
    }
}

private fun Git.setTag(tagName: String) {
    Git(repository).use { git ->
        git.tag().setName(tagName).setMessage(tagName).setAnnotated(true).call()
    }
}
