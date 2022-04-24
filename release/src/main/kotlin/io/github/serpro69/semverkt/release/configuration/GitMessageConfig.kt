package io.github.serpro69.semverkt.release.configuration

/**
 * Git message configuration. Used for determining next version increment based on git messages.
 *
 * @property major      keyword for bumping major version
 * @property minor      keyword for bumping minor version
 * @property patch      keyword for bumping patch version
 * @property preRelease keyword for bumping preRelease version
 * @property ignoreCase controls whether the keywords should be case-sensitive or not
 */
interface GitMessageConfig {
    val major: String get() = "[major]"
    val minor: String get() = "[minor]"
    val patch: String get() = "[patch]"
    val preRelease: String get() = "[pre release]"
    val ignoreCase: Boolean get() = false
}
