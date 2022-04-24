package io.github.serpro69.semverkt.release.configuration

/**
 * Git tag configuration
 *
 * @property prefix         the prefix of a git tag which denotes a release version; defaults to `v`
 * @property separator      separator between the [prefix] and the version; defaults to an empty string
 * @property useBranches    whether to use branches to determine releases, instead of tags; defaults to `false`
 */
interface GitTagConfig {
    val prefix: String get() = "v"
    val separator: String get() = ""
    val useBranches: Boolean get() = false
}
