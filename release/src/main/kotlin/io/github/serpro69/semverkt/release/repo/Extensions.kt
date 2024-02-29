package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.release.ext.tail
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import java.time.LocalDateTime
import java.time.ZoneId

internal val semver: (prefix: TagPrefix) -> (Ref) -> Semver = {
    { ref -> Semver(ref.simpleTagName.replace(it.toString(), "")) }
}

internal val Ref.simpleTagName: String
    get() = name.replace(Regex("""^refs/tags/"""), "")

internal val RevCommit.dateTime: LocalDateTime
    get() = authorIdent.`when`.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

internal val RevCommit.message: Message
    get() = Message(title, description)

/**
 * @property title the title of this git commit
 */
internal val RevCommit.title: String
    get() = this.shortMessage

/**
 * @property description the message for this git commit without the first line (title)
 *
 * The returned list only contains non-empty lines from the full commit message minus the title (first line of the commit)
 */
internal val RevCommit.description: List<String>
    get() = this.fullMessage.split("\n").tail().mapNotNull {
        it.trim().ifEmpty { null }
    }
