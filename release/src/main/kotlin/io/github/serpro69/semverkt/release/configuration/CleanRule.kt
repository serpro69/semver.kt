package io.github.serpro69.semverkt.release.configuration

/**
 * Enum class that defines the rules for checking if the git repository is clean before creating a release.
 *
 * - [ALL] checks all files in the repository
 * - [TRACKED] checks only tracked files in the repository
 * - [NONE] ignores any changes in the repository
 */
enum class CleanRule {
    ALL,
    TRACKED,
    NONE,
    ;

    companion object {
        fun getByName(name: String): CleanRule {
            return enumValues<CleanRule>().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
        }
    }

    override fun toString(): String {
        return name.lowercase()
    }
}
