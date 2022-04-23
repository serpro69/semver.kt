package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.ConfigurationProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

class GitRepository(private val config: ConfigurationProvider) : Repository {
    private val git: Git by lazy { Git.open(config.git.repo.directory.toFile()) }
    private val tags: () -> List<Ref> = {
        git.tagList().call().filter { it.name.startsWith("refs/tags/${config.git.tag.prefix}") }
    }
    override val lastVersion: () -> Ref? = {
        val tags = tags()
        if (tags.isNotEmpty()) tags.last() else null
    }

    override fun log(untilTag: Ref?, predicate: (RevCommit) -> Boolean): List<Commit> {
        val objectId = untilTag?.let { git.repository.refDatabase.peel(it)?.peeledObjectId ?: it.objectId }
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
                    val tagId: ObjectId = git.repository.refDatabase.peel(ref)?.peeledObjectId ?: ref.objectId
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

    private fun log(start: ObjectId? = null, end: ObjectId? = null): Sequence<RevCommit> {
        val log = git.log()
        if (start != null) {
            if (end != null) log.addRange(start, end) else log.add(start)
        } else if (end != null) log.not(end)
        return log.call().asSequence()
    }
}

