package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.ConfigurationProvider
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

/**
 * An interface representation of a repository
 *
 * @property config [ConfigurationProvider] for this repository.
 */
interface Repository : AutoCloseable {

    val config: ConfigurationProvider

    /**
     * Returns the `HEAD` of this repository.
     */
    val head: () -> ObjectId

    /**
     * Returns a list of tag [Ref]s from this repository.
     */
    val tags: () -> List<Ref>

    /**
     * Returns the latest tag from this repository.
     *
     * The implementor should decide (and document) what the "LATEST" means and how it is calculated.
     */
    val latestVersionTag: () -> Ref?

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
