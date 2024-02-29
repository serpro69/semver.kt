package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.IOException

/**
 * Implementation of a git [Repository].
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
    private val sv: (TagPrefix?) -> (Ref) -> Semver = { semver(it ?: config.git.tag.prefix) }

    override val head: () -> ObjectId = { git.repository.resolve(Constants.HEAD) }

    /**
     * Returns a list of (potentially peeled) tag [Ref]s from this repository,
     * optionally filtered by the git tag [prefix] string.
     *
     * If [prefix] is `null`, [GitTagConfig.prefix] will be used instead.
     */
    override fun tags(prefix: TagPrefix?): List<Ref> {
        val p = prefix ?: config.git.tag.prefix
        return git.tagList().call()
            .filter {
                it.name.startsWith("refs/tags/$p")
                    && Semver.isValid(it.simpleTagName.substringAfter(p.toString()))
            }.map { it.peel() }
    }

    override fun headVersionTag(prefix: TagPrefix?): Ref? = tags(prefix).firstOrNull {
        (it.peeledObjectId ?: it.objectId) == head().toObjectId()
    }

    /**
     * Returns the latest tag from this git repository.
     *
     * The tags are filtered by the [GitTagConfig.prefix],
     * unless the [prefix] argument is NOT `null`,
     * and tags that don't start with the specified prefix are omitted.
     *
     * The tags are compared using [Semver.compareTo], and the tag with maximum version is returned.
     */
    override fun latestVersionTag(prefix: TagPrefix?): Ref? {
        val tags = tags(prefix)
        return when {
            tags.isNotEmpty() -> {
                val comparator: (Ref, Ref) -> Int = { o1: Ref, o2: Ref ->
                    sv(prefix)(o1).compareTo(sv(prefix)(o2))
                }
                tags.maxOfWith(comparator) { it }
            }
            else -> null
        }
    }

    /**
     * Whether this [GitRepository] status is clean.
     *
     * @return `true` if no differences exist between the working-tree, the index, and the current HEAD,
     * and `false` if differences do exist
     */
    override val isClean: () -> Boolean = {
        git.status().call().isClean
    }

    override val hasUncommittedChanges: () -> Boolean = {
        git.status().call().hasUncommittedChanges()
    }

    /**
     * Returns a log of [Commit]s from HEAD and [untilTag] ref, with an optional [predicate] to filter out the commits.
     *
     * @param tagPrefix an optional tag prefix to filter by
     */
    override fun log(untilTag: Ref?, tagPrefix: TagPrefix?, predicate: (RevCommit) -> Boolean): List<Commit> {
        val objectId = untilTag?.let { it.peel().peeledObjectId ?: it.objectId }
        return log(end = objectId, tagPrefix = tagPrefix, predicate = predicate)
    }

    override fun log(
        start: ObjectId?,
        end: ObjectId?,
        tagPrefix: TagPrefix?,
        predicate: (RevCommit) -> Boolean,
    ): List<Commit> {
        val tags = tags(tagPrefix)
        val commits: List<Commit> = log(start = start, end = end).fold(mutableListOf()) { acc, commit ->
            if (predicate(commit)) {
                val tag = tags.lastOrNull { ref ->
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

    override fun diff(start: String, end: String): List<DiffEntry> {
        // change diff algorithm to histogram to avoid potential error 'invalid value diff.algorithm=patience'
        // patience algorithm doesn't seem to be supported by jgit
        val s = git.repository.resolve("$start^{tree}")
        val e = git.repository.resolve("$end^{tree}")
        git.repository.config.baseConfig.setString("diff", null, "algorithm", "histogram")
        return git.repository.newObjectReader().use { reader ->
            val oldTreeIter = CanonicalTreeParser().also { it.reset(reader, s) }
            val newTreeIter = CanonicalTreeParser().also { it.reset(reader, e) }
            Git(git.repository).use { git ->
                git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call()
                    .map { DiffEntry(oldPath = it.oldPath, newPath = it.newPath) }
            }
        }
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

