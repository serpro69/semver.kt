package io.github.serpro69.semverkt.gradle.plugin.tasks

import io.github.serpro69.semverkt.gradle.plugin.SemverKtPluginConfig
import io.github.serpro69.semverkt.gradle.plugin.SemverKtPluginGitTagConfig
import io.github.serpro69.semverkt.release.configuration.CleanRule
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
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
            next != null && next.toString().endsWith(config.get().version.snapshotSuffix) -> {
                logger.log(LIFECYCLE, "Snapshot version, not doing anything")
            }
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
            // check if tag exists, don't try to create a duplicate
            val tagExists = GitRepository(config).use { repo ->
                // check if repo is clean
                when (config.git.repo.cleanRule) {
                    CleanRule.ALL -> if (!repo.isClean()) throw GradleException("Release with non-clean repository is not allowed")
                    CleanRule.TRACKED -> if (repo.hasUncommittedChanges()) throw GradleException("Release with uncommitted changes is not allowed")
                    CleanRule.NONE -> { /* noop */ }
                }
                repo.tags().any {
                    Semver(it.name.replace(Regex("""^refs/tags/${config.git.tag.prefix}"""), "")) == nextVer
                }
            }
            val t = "${config.git.tag.prefix}$nextVer"
            if (!tagExists) {
                logger.log(DEBUG, "Open repo at: $this")
                FileRepositoryBuilder()
                    .setWorkTree(project.projectDir)
                    .findGitDir(project.projectDir)
                    .build()
                    .use {
                        logger.log(DEBUG, "Set tag to: $t")
                        Git(it).use { git -> git.setTag(t, config.git.tag) }
                    }
            } else logger.log(LIFECYCLE, "Tag $t already exists in project")
        }
    }
}

private fun Git.setTag(tagName: String, config: SemverKtPluginGitTagConfig) {
    with(tag()) {
        setName(tagName)
        if (config.message.isNotBlank()) setMessage(config.message).setAnnotated(true)
        call()
    }
}
