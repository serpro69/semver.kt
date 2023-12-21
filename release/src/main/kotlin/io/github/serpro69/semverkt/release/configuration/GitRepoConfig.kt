package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.configuration.CleanRule.TRACKED
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Git repository configuration
 *
 * @property directory  local path of a git repository; defaults to `.`
 * @property remoteName name of the remote of this git repository; defaults to `origin`
 * @property cleanRule  check for changes in the repository before creating a release;
 * defaults to [CleanRule.TRACKED], which means that only tracked files will be checked
 */
interface GitRepoConfig {
    val directory: Path get() = Path(".")
    val remoteName: String get() = "origin"
    val cleanRule: CleanRule get() = TRACKED

    /**
     * Returns a json string representation of this [GitRepoConfig] instance.
     */
    fun jsonString(): String {
        return """
            "repo": { "directory": "$directory", "remoteName": "$remoteName", "cleanRule": "$cleanRule" }
        """.trimIndent()
    }
}
