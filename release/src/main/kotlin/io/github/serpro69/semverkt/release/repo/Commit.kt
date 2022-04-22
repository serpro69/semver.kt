package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.lib.ObjectId
import java.time.LocalDateTime

/**
 * Represents a commit in a repository.
 *
 * @property objectId   a commit object id
 * @property message    a commit message
 * @property dateTime   the date and time of the commit
 * @property version    a semantic version associated with the commit
 */
data class Commit(
    val objectId: ObjectId,
    val message: Message,
    val dateTime: LocalDateTime,
    val version: Semver? = null,
)

/**
 * Represents a commit message.
 *
 * @property title          a title of the commit (first line followed by an empty line)
 * @property description    the optional longer description of the commit (commitMessage minus the title)
 */
data class Message(
    val title: String,
    val description: List<String>,
)
