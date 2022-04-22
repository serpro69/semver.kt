package io.github.serpro69.semverkt.release.repo

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit

interface Repository {
    fun log(
        start: ObjectId? = null,
        end: ObjectId? = null,
        predicate: (RevCommit) -> Boolean = { true }
    ): Log
}
