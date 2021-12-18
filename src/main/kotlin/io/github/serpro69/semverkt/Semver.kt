package io.github.serpro69.semverkt

import kotlin.math.sign

data class Semver(private val version: String) {
    init {
        if (!isValid()) throw IllegalVersionException("'$version' is not a valid semver version.")
    }

    constructor(major: Int, minor: Int, patch: Int) : this("$major.$minor.$patch")

    private fun isValid(): Boolean {
        return version.split(".")
            .map {
                when {
                    it.length > 1 && it.startsWith("0") -> {
                        throw IllegalVersionException("'$version' numbers MUST NOT contain leading zeroes.")
                    }
                    else -> it.toInt()
                }
            }
            .none { it.sign == -1 }
    }
}
