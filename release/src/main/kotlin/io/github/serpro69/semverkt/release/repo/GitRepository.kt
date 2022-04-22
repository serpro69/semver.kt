package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.ConfigurationProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import java.time.ZoneId

class GitRepository(private val config: ConfigurationProvider) : Repository {
    private val git: Git = Git.open(config.git.repo.directory.toFile())

    override fun log(
        start: ObjectId?,
        end: ObjectId?,
        predicate: (RevCommit) -> Boolean,
    ): Log {
        val versionedCommits = git.tagList().call().map { ref ->
            val configuredVer = semver(config.git.tag)
            val ver = configuredVer(ref)
            git.repository.refDatabase.peel(ref).peeledObjectId?.let { it to ver }
                ?: (ref.objectId to ver)
        }

        val commits: List<Commit> = log(start = start, end = end).fold(mutableListOf()) { acc, commit ->
            if (predicate(commit)) {
                val c = Commit(
                    objectId = commit.id,
                    message = Message(
                        title = commit.title,
                        description = commit.description
                    ),
                    dateTime = commit.authorIdent.`when`.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    version = versionedCommits.firstOrNull { it.first == commit.id }?.second
                )
                acc.add(c)
            }

            acc
        }

        return Log(commits)
    }

    private fun log(start: ObjectId? = null, end: ObjectId? = null): Sequence<RevCommit> {
        val log = git.log()
        if (start != null) {
            if (end != null) log.addRange(start, end) else log.add(start)
        } else if (end != null) log.not(end)
        return log.call().asSequence()
    }
}

