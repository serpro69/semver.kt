package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit

/**
 * An interface representation of a repository
 */
interface Repository {

    /**
     * Returns a [Log] of commits from this repository.
     */
    fun log(
        start: ObjectId? = null,
        end: ObjectId? = null,
        predicate: (RevCommit) -> Boolean = { true }
    ): Log

    /**
     * Returns the latest [Commit] that has a tagged version release,
     * or `null` if no releases exist.
     */
    fun latestVersionedCommit(): Commit? = log().commits.firstOrNull { it.version != null }
}
