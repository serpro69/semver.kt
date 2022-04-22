package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.lib.ObjectId
import java.time.LocalDateTime

data class Commit(
    val objectId: ObjectId,
    val message: Message,
    val dateTime: LocalDateTime,
    val version: Semver? = null,
)

data class Message(
    val title: String,
    val description: List<String>,
)
