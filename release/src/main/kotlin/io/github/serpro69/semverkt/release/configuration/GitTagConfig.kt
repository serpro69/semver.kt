package io.github.serpro69.semverkt.release.configuration

/**
 * Git tag configuration
 *
 * @property prefix         the prefix of a git tag which denotes a release version; defaults to `v`
 * @property separator      separator between the [prefix] and the version; defaults to an empty string
 * @property useBranches    whether to use branches to determine releases, instead of tags; defaults to `false`
 */
interface GitTagConfig {
    val prefix: TagPrefix get() = TagPrefix("v")
    val separator: String get() = ""
    val useBranches: Boolean get() = false

    /**
     * Returns a json string representation of this [GitTagConfig] instance.
     */
    fun jsonString(): String {
        return """
            "tag": { "prefix": "$prefix", "separator": "$separator", "useBranches": "$useBranches" }
        """.trimIndent()
    }
}

@JvmInline
value class TagPrefix(private val value: String) {

    override fun toString(): String = value
}
