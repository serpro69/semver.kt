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
