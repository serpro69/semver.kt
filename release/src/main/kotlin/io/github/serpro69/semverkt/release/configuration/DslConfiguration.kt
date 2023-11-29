package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import java.nio.file.Path

/**
 * Provides access to default [Configuration] properties with optional overrides through DSL syntax
 */
class DslConfiguration internal constructor() : Configuration {

    override var git = PojoGitConfig()
        private set
    override var version = PojoVersionConfig()
        private set
    override var monorepo = PojoMonorepoConfig()
        private set

    fun git(block: PojoGitConfig.() -> Unit): Configuration {
        git.apply(block)
        return this
    }

    fun version(block: PojoVersionConfig.() -> Unit): Configuration {
        version.apply(block)
        return this
    }

    fun monorepo(block: PojoMonorepoConfig.() -> Unit): Configuration {
        monorepo.apply(block)
        return this
    }
}

@PojoConfigDsl
class PojoMonorepoConfig internal constructor() : MonorepoConfig {
    override var modules: List<ModuleConfig> = super.modules
}

@PojoConfigDsl
class PojoVersionConfig internal constructor() : VersionConfig {
    override var initialVersion: Semver = super.initialVersion
    override var placeholderVersion: Semver = super.placeholderVersion
    override var defaultIncrement: Increment = super.defaultIncrement
    override var preReleaseId: String = super.preReleaseId
    override var initialPreRelease: Int = super.initialPreRelease
    override var snapshotSuffix: String = super.snapshotSuffix
    var useSnapshots: Boolean = false
}

@PojoConfigDsl
class PojoGitConfig internal constructor() : GitConfig {
    override val repo = PojoGitRepoConfig()
    override val tag = PojoGitTagConfig()
    override val message = PojoGitMessageConfig()

    fun repo(block: PojoGitRepoConfig.() -> Unit) {
        repo.apply(block)
    }

    fun tag(block: PojoGitTagConfig.() -> Unit) {
        tag.apply(block)
    }

    fun message(block: PojoGitMessageConfig.() -> Unit) {
        message.apply(block)
    }
}

@PojoConfigDsl
class PojoGitRepoConfig internal constructor() : GitRepoConfig {

    override var directory: Path = super.directory
    override var remoteName: String = super.remoteName
}

@PojoConfigDsl
class PojoGitTagConfig internal constructor() : GitTagConfig {

    override var prefix: String = super.prefix
    override var separator: String = super.separator
    override var useBranches: Boolean = super.useBranches
}

@PojoConfigDsl
class PojoGitMessageConfig internal constructor() : GitMessageConfig {

    override var major: String = super.major
    override var minor: String = super.minor
    override var patch: String = super.patch
    override var preRelease: String = super.preRelease
    override var ignoreCase: Boolean = super.ignoreCase
}

@DslMarker
annotation class PojoConfigDsl

fun DslConfiguration(block: DslConfiguration.() -> Unit): DslConfiguration {
    return DslConfiguration().apply(block)
}
