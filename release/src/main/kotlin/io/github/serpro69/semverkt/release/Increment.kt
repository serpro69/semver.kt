package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.Increment.DEFAULT
import io.github.serpro69.semverkt.release.Increment.MAJOR
import io.github.serpro69.semverkt.release.Increment.MINOR
import io.github.serpro69.semverkt.release.Increment.PATCH
import io.github.serpro69.semverkt.release.Increment.PRE_RELEASE
import io.github.serpro69.semverkt.release.configuration.VersionConfig

/**
 * The increment for the next version, where [MAJOR].[MINOR].[PATCH]-[PRE_RELEASE] correspond to the semantic version components,
 * and [DEFAULT] is configured through [VersionConfig.defaultIncrement] configuration property.
 */
enum class Increment {
    MAJOR,
    MINOR,
    PATCH,
    PRE_RELEASE,
    DEFAULT,
    NONE,
    ;

    companion object {
        fun getByName(name: String): Increment {
            return enumValues<Increment>().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
        }
    }

    override fun toString(): String = name.lowercase()
}
