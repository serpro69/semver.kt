package io.github.serpro69.semverkt.release.configuration

/**
 * Git tag configuration
 *
 * @property prefix         the prefix of a git tag which denotes a release version; defaults to `v`
 * @property message        the message for the annotated git tag; defaults to an empty string
 * @property separator      separator between the [prefix] and the version; defaults to an empty string
 * @property useBranches    whether to use branches to determine releases, instead of tags; defaults to `false`
 */
interface GitTagConfig {
    val prefix: String get() = "v"
    val message: String get() = ""
    val separator: String get() = ""
    val useBranches: Boolean get() = false

    /**
     * Returns a json string representation of this [GitTagConfig] instance.
     */
    fun jsonString(): String {
        return """
            "tag": { "prefix": "$prefix", "message": "$message", "separator": "$separator", "useBranches": "$useBranches" }
        """.trimIndent()
    }
}
