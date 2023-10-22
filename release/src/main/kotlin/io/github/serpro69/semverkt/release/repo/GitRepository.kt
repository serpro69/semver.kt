package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import java.io.IOException

/**
 * Represents a git [Repository] implementation.
 */
class GitRepository(override val config: Configuration) : Repository {
    private val git: Git by lazy {
        val repoDir = config.git.repo.directory.toFile()
        try {
            Git.open(repoDir)
        } catch (e: IOException) {
            throw IOException("Can't open $repoDir as git repository", e)
        }
    }

    override val head: () -> ObjectId = { git.repository.resolve(Constants.HEAD) }

    /**
     * Returns a list of (potentially peeled) tag [Ref]s from this repository,
     * filtered by the [GitTagConfig.prefix]
     */
    override val tags: () -> List<Ref> = {
        git.tagList().call()
            .filter { it.name.startsWith("refs/tags/${config.git.tag.prefix}") }
            .map { it.peel() }
    }

    /**
     * Returns the latest tag from this git repository.
     *
     * The tags are filtered by the [GitTagConfig.prefix],
     * and tags that don't start with the specified prefix are omitted.
     *
     * The tags are compared using [Semver.compareTo], and the tag with maximum version is returned.
     */
    override val latestVersionTag: () -> Ref? = {
        val tags = tags()
        if (tags.isNotEmpty()) {
            val comparator: (Ref, Ref) -> Int = { o1: Ref, o2: Ref ->
                semver(config.git.tag)(o1).compareTo(semver(config.git.tag)(o2))
            }
            tags.maxOfWith(comparator) { it }
        } else null
    }

    /**
     * Returns a log of [Commit]s from HEAD and [untilTag] ref, with an optional [predicate] to filter out the commits.
     */
    override fun log(untilTag: Ref?, predicate: (RevCommit) -> Boolean): List<Commit> {
        val objectId = untilTag?.let { it.peel().peeledObjectId ?: it.objectId }
        return log(end = objectId, predicate = predicate)
    }

    override fun log(
        start: ObjectId?,
        end: ObjectId?,
        predicate: (RevCommit) -> Boolean,
    ): List<Commit> {
        val commits: List<Commit> = log(start = start, end = end).fold(mutableListOf()) { acc, commit ->
            if (predicate(commit)) {
                val tag = tags().lastOrNull { ref ->
                    val tagId: ObjectId = ref.peeledObjectId ?: ref.objectId
                    commit.id == tagId
                }
                val c = Commit(
                    objectId = commit.id,
                    message = commit.message,
                    dateTime = commit.dateTime,
                    tag = tag
                )
                acc.add(c)
            }

            acc
        }

        return commits
    }

    override fun close() {
        git.close()
    }

    private fun log(start: ObjectId? = null, end: ObjectId? = null): Sequence<RevCommit> {
        val log = git.log()
        if (start != null) {
            if (end != null) log.addRange(start, end) else log.add(start)
        } else if (end != null) log.not(end)
        return log.call().asSequence()
    }

    private fun Ref.peel(): Ref = git.repository.refDatabase.peel(this)
}

