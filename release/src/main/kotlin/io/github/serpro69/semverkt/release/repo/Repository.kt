package io.github.serpro69.semverkt.release.repo

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

/**
 * An interface representation of a repository
 */
interface Repository {

    val lastVersion: () -> Ref?

    /**
     * Returns a list of [Commit]s in this repository,
     * with an optional [predicate] to filter the commits.
     *
     * If both [start] and [end] are provided, uses the range `since..until` for the git log.
     *
     * @param start the commit to start git log graph traversal from
     * @param end same as `--not start` or `start^`
     */
    fun log(
        start: ObjectId? = null,
        end: ObjectId? = null,
        predicate: (RevCommit) -> Boolean = { true }
    ): List<Commit>

    /**
     * Returns a list of [Commit]s in this repository,
     * with an optional [predicate] to filter the commits.
     */
    fun log(
        untilTag: Ref?,
        predicate: (RevCommit) -> Boolean = { true }
    ): List<Commit>
}
