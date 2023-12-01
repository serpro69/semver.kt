package io.github.serpro69.semverkt.release.configuration

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Git repository configuration
 *
 * @property directory  local path of a git repository; defaults to `.`
 * @property remoteName name of the remote of this git repository; defaults to `origin`
 */
interface GitRepoConfig {
    val directory: Path get() = Path(".")
    val remoteName: String get() = "origin"

    /**
     * Returns a json string representation of this [GitRepoConfig] instance.
     */
    fun jsonString(): String {
        return """
            "repo": { "directory": "$directory", "remoteName": "$remoteName" }
        """.trimIndent()
    }
}
