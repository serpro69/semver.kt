package io.github.serpro69.semverkt.release

enum class Increment {
    MAJOR,
    MINOR,
    PATCH,
    PRE_RELEASE,
    DEFAULT,
    ;

    companion object {
        fun getByName(name: String): Increment {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
        }
    }

    override fun toString(): String = name.lowercase()
}

