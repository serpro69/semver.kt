package io.github.serpro69.semverkt.release.ext

/**
 * Returns this list of [T] element minus the `head` element.
 */
internal fun <T> List<T>.tail() : List<T> {
    return if (this.isEmpty()) emptyList() else this.takeLast(size-1)
}
