package io.github.serpro69.semverkt.release.configuration

/**
 * Provides access properties for automated releases configuration.
 *
 * @property git      git configuration
 * @property version  version configuration
 * @property monorepo monorepo configuration
 */
interface Configuration {
    val git: GitConfig
    val version: VersionConfig
    val monorepo: MonorepoConfig

    /**
     * Returns a json string representation of this [Configuration] instance.
     */
    fun jsonString(): String {
        return """
            { ${git.jsonString()}, ${version.jsonString()}, ${monorepo.jsonString()} }
        """.trimIndent()
    }
}

/**
 * Provides access to default [Configuration] properties with optional overrides through plain java objects
 */
class PojoConfiguration(
    gitRepoConfig: GitRepoConfig = object : GitRepoConfig {},
    gitTagConfig: GitTagConfig = object : GitTagConfig {},
    gitMessageConfig: GitMessageConfig = object : GitMessageConfig {},
    versionConfig: VersionConfig = object : VersionConfig {},
    monorepoConfig: MonorepoConfig = object : MonorepoConfig {},
): Configuration {
    override val git: GitConfig = object : GitConfig {
        override val repo: GitRepoConfig = gitRepoConfig
        override val tag: GitTagConfig = gitTagConfig
        override val message: GitMessageConfig = gitMessageConfig
    }
    override val version: VersionConfig = versionConfig
    override val monorepo: MonorepoConfig = monorepoConfig
}
