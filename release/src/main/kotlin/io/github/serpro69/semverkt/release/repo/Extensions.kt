package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.ext.tail
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

internal val semver: (GitTagConfig) -> (Ref) -> Semver = { config ->
    { ref -> Semver(ref.name.replace("refs/tags/${config.prefix}", "")) }
}

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
