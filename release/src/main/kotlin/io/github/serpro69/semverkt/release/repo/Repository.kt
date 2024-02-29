package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
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
     *
     * @param prefix an optional prefix to filter tags by.
     */
    fun tags(prefix: TagPrefix? = null): List<Ref>

    /**
     * Returns the latest tag from this repository.
     *
     * The implementor should decide (and document) what the "LATEST" means and how it is calculated.
     *
     * @param prefix an optional prefix for the tag
     */
    fun latestVersionTag(prefix: TagPrefix? = null): Ref?

    /**
     * Returns tag pointing at HEAD of this repository.
     *
     * @param prefix an optional prefix for the tag
     */
    fun headVersionTag(prefix: TagPrefix? = null): Ref?

    /**
     * Returns `true` if no differences exist in the repository, and `false` otherwise
     */
    val isClean: () -> Boolean

    /**
     * Returns `true` if any tracked file is changed, and `false` otherwise.
     */
    val hasUncommittedChanges: () -> Boolean

    /**
     * Returns a list of [Commit]s in this repository,
     * with an optional [predicate] to filter the commits.
     *
     * If both [start] and [end] are provided, uses the range `since..until` for the git log.
     *
     * @param start     the commit to start git log graph traversal from
     * @param end       same as `--not start` or `start^`
     * @param tagPrefix an optional tag prefix to filter by
     */
    fun log(
        start: ObjectId? = null,
        end: ObjectId? = null,
        tagPrefix: TagPrefix? = null,
        predicate: (RevCommit) -> Boolean = { true },
    ): List<Commit>

    /**
     * Returns a list of [Commit]s in this repository,
     * with an optional [predicate] to filter the commits.
     *
     * @param tagPrefix an optional tag prefix to filter by
     */
    fun log(
        untilTag: Ref?,
        tagPrefix: TagPrefix? = null,
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
