package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.ConfigurationProvider
import io.github.serpro69.semverkt.release.repo.Commit
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Message
import io.github.serpro69.semverkt.release.repo.Repository

class SemverRelease(private val configuration: ConfigurationProvider) {
    private val repo: Repository = GitRepository(configuration)

    fun List<Commit>.computeNextIncrement(): Increment {
        var inc = Increment.NONE
        forEach { c ->
            if (c.message.full().contains(configuration.git.message.major)) return Increment.MAJOR
            if (inc > Increment.MINOR && c.message.full().contains(configuration.git.message.minor)) {
                inc = Increment.MINOR
                return@forEach
            }
            if (inc > Increment.PATCH && c.message.full().contains(configuration.git.message.patch)) {
                inc = Increment.PATCH
                return@forEach
            }
            if (inc > Increment.PRE_RELEASE && c.message.full().contains(configuration.git.message.preRelease)) {
                inc = Increment.PRE_RELEASE
            }
        }
        return inc
    }

    private fun Message.full(): String {
        return with(StringBuilder()) {
            appendLine(title)
            appendLine()
            appendLine(description.joinToString("\n"))
            toString()
        }
    }
}

