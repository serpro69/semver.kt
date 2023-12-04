package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.Configuration
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit

/**
 * An interface representation of a repository
 *
 * @property config [Configuration] for this repository.
 */
interface Repository : AutoCloseable {

    val config: Configuration

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
     * Returns tag pointing at HEAD of this repository.
     */
    val headVersionTag: () -> Ref?

    /**
     * Returns `true` if no differences exist in the repository, and `false` otherwise
     */
    val isClean: () -> Boolean

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

    /**
     * Returns the diff between [start] and [end] commits in this repository.
     *
     * These operators are supported for the [start] and [end] parameters:
     * - **HEAD**, **MERGE_HEAD**, **FETCH_HEAD**
     * - **SHA-1**: a complete or abbreviated SHA-1
     * - **refs/...**: a complete reference name
     * - **short-name**: a short reference name under `refs/heads`
     * - `refs/tags`, or `refs/remotes` namespace
     * - `ABBREV` as an abbreviated SHA-1.
     */
    fun diff(start: String, end: String): List<DiffEntry>
}
