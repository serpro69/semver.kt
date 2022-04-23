package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.ConfigurationProvider
import io.github.serpro69.semverkt.release.repo.Commit
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Message
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.release.repo.semver
import io.github.serpro69.semverkt.spec.PreRelease
import io.github.serpro69.semverkt.spec.Semver

class SemverRelease {
    private val repo: Repository
    private val config: ConfigurationProvider
    private val currentVersion: () -> Semver?

    constructor(configuration: ConfigurationProvider) {
        repo = GitRepository(configuration)
        config = configuration
        currentVersion = { repo.lastVersion()?.let { semver(configuration.git.tag)(it) } }
    }

    constructor(repository: Repository) {
        repo = repository
        config = repo.config
        currentVersion = { repo.lastVersion()?.let { semver(config.git.tag)(it) } }
    }

    fun increment(increment: Increment, release: Boolean = true): Semver {
        val nextVersion = currentVersion()?.let {
            when (increment) {
                Increment.MAJOR -> it.incrementMajor()
                Increment.MINOR -> it.incrementMinor()
                Increment.PATCH -> it.incrementPatch()
                Increment.PRE_RELEASE -> it.incrementPreRelease()
                Increment.DEFAULT -> {
                    it.preRelease?.let { _ -> it.incrementPreRelease() }
                        ?: increment(config.version.defaultIncrement, release)
                }
            }
        } ?: config.version.initialVersion
        // TODO check if nextVersion != currentVersion
        return if (release) nextVersion else nextVersion.withSnapshot()
    }

    fun createPreRelease(increment: Increment): Semver {
        println(repo.lastVersion())
        println(currentVersion())
        return currentVersion()?.preRelease?.let { currentVersion() } ?: run {
            val preRelease = PreRelease("${config.version.preReleaseId}.1")
            increment(increment, true).copy(preRelease = preRelease)
        }
    }

    fun nextIncrement(): Increment = repo.log(repo.lastVersion()).nextIncrement()

    private fun List<Commit>.nextIncrement(): Increment {
        var inc = Increment.DEFAULT
        forEach { c ->
            if (c.message.full().contains(config.git.message.major)) return Increment.MAJOR
            if (inc > Increment.MINOR && c.message.full().contains(config.git.message.minor)) {
                inc = Increment.MINOR
                return@forEach
            }
            if (inc > Increment.PATCH && c.message.full().contains(config.git.message.patch)) {
                inc = Increment.PATCH
                return@forEach
            }
            if (inc > Increment.PRE_RELEASE && c.message.full().contains(config.git.message.preRelease)) {
                inc = Increment.PRE_RELEASE
            }
        }
        return inc
    }

    private fun Semver.withSnapshot(): Semver {
        val pr = preRelease?.toString()?.let {
            when {
                it.endsWith(config.version.snapshotSuffix) -> it
                else -> "${it}-${config.version.snapshotSuffix}"
            }
        } ?: config.version.snapshotSuffix
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

