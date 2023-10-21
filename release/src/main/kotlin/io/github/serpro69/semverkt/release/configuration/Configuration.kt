package io.github.serpro69.semverkt.release.configuration

/**
 * Provides access properties for automated releases configuration.
 *
 * @property git git configuration
 */
interface Configuration {
    val git: GitConfig
    val version: VersionConfig

    fun jsonString(): String {
        return """
            { ${git.jsonString()}, ${version.jsonString()} }
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
    versionConfig: VersionConfig = object : VersionConfig {}
): Configuration {
    override val git: GitConfig = object : GitConfig {
        override val repo: GitRepoConfig = gitRepoConfig
        override val tag: GitTagConfig = gitTagConfig
        override val message: GitMessageConfig = gitMessageConfig
    }
    override val version: VersionConfig = versionConfig
}
