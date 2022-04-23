package io.github.serpro69.semverkt.release.configuration

interface GitMessageConfig {
    val major: String get() = "[major]"
    val minor: String get() = "[minor]"
    val patch: String get() = "[patch]"
    val preRelease: String get() = "[pre release]"
    val ignoreCase: Boolean get() = false
}
