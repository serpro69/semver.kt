package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.ConfigurationProvider
import io.github.serpro69.semverkt.release.repo.Commit
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Message
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.spec.PreRelease
import io.github.serpro69.semverkt.spec.Semver

class SemverRelease(private val configuration: ConfigurationProvider) {
    private val repo: Repository = GitRepository(configuration)

    fun Semver.increment(increment: Increment, release: Boolean = true): Semver {
        val nextVersion = when (increment) {
            Increment.MAJOR -> incrementMajor()
            Increment.MINOR -> incrementMinor()
            Increment.PATCH -> incrementPatch()
            Increment.PRE_RELEASE -> incrementPreRelease()
            Increment.DEFAULT -> {
                preRelease?.let { _ -> increment(Increment.PRE_RELEASE, release) }
                    ?: increment(configuration.version.defaultIncrement, release)
            }
        }
        return if (release) nextVersion else nextVersion.withSnapshot()
    }

    fun Semver.createPreRelease(): Semver {
        return preRelease?.let { this } ?: run {
            val preRelease = PreRelease("${configuration.version.preReleaseId}.1")
            increment(configuration.version.defaultIncrement, false).copy(preRelease = preRelease)
        }
    }

    fun List<Commit>.nextIncrement(): Increment {
        var inc = Increment.DEFAULT
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

    private fun Semver.withSnapshot(): Semver {
        val pr = preRelease?.toString()?.let {
            when {
                it.endsWith(configuration.version.snapshotSuffix) -> it
                else -> "${it}-${configuration.version.snapshotSuffix}"
            }
        } ?: configuration.version.snapshotSuffix
        return copy(preRelease = PreRelease(pr), buildMetadata = null)
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

