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
) {

    /*
     * Override equals because [tag] comparison can sometimes return `false` even if they're "technically" the same tag.
     * This is because they're not immutable.
     *
     * For example, if we first peel the tag, and then compare it to self in the same [Repository] instance,
     * then the default equals implementation would return `false`
     *
     * We want to avoid such behavior because we need a "logical" comparison of tags.
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            !is Commit -> false
            else -> {
                objectId.equals(other.objectId) &&
                    message == other.message &&
                    dateTime.isEqual(other.dateTime) &&
                    tag?.let {
                        when (other.tag) {
                            null -> false
                            else -> it.name == other.tag.name &&
                                it.simpleTagName == other.tag.simpleTagName &&
                                it.objectId.equals(other.tag.objectId) &&
                                it.isPeeled == other.tag.isPeeled &&
                                (it.peeledObjectId?.equals(other.tag.peeledObjectId) ?: true) &&
                                it.isSymbolic == other.tag.isSymbolic
                        }
                    } ?: true
            }
        }
    }

    override fun hashCode(): Int {
        var result = objectId.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + dateTime.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        return result
    }
}

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

/**
 * Represents a change to a file between two commits.
 *
 * @property oldPath File name of the old (pre-image).
 *
 * The meaning of the old name can differ depending on the semantic meaning of this patch:
 * - *file add*: always `/dev/null`
 * - *file modify*: always [newPath]
 * - *file delete*: always the file being deleted
 * - *file copy*: source file the copy originates from
 * - *file rename*: source file the rename originates from
 *
 * @property newPath File name of the new (post-image).
 *
 * The meaning of the new name can differ depending on the semantic meaning of this patch:
 * - *file add*: always the file being created
 * - *file modify*: always [oldPath]
 * - *file delete*: always `/dev/null`
 * - *file copy*: destination file the copy ends up at
 * - *file rename*: destination file the rename ends up at
 */
data class DiffEntry(
    val oldPath: String,
    val newPath: String,
)
