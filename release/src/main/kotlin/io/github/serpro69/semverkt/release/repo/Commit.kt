package io.github.serpro69.semverkt.release.repo

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import java.time.LocalDateTime

/**
 * Represents a commit in a repository.
 *
 * @property objectId   a commit object id
 * @property message    a commit message
 * @property dateTime   the date and time of the commit
 * @property tag        a git tag associated with the commit
 */
data class Commit(
    val objectId: ObjectId,
    val message: Message,
    val dateTime: LocalDateTime,
    val tag: Ref?,
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
