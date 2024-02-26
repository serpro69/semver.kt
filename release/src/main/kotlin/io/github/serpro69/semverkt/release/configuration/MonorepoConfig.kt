package io.github.serpro69.semverkt.release.configuration

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Monorepo module configuration enables individual versioning of multi-modules in a mono-repo.
 *
 * @property modules mono-repo modules
 */
interface MonorepoConfig {
    val modules: List<ModuleConfig> get() = emptyList()

    fun jsonString(): String {
        return """
            "monorepo": [ ${modules.joinToString(", ") { it.jsonString() }} ]
        """.trimIndent()
    }
}

/**
 * Configuration of a single module in a multi-module mono-repo.
 *
 * @property name       module directory name (e.g. `core` for `./core` submodule in the root of the mono-repo)
 * @property sources    path to track changes for the module, relative to the module dir [name].
 * Defaults to module directory.
 * @property tag        git tag configuration for the module.
 */
interface ModuleConfig {
    val name: String
    val sources: Path get() = Path(".")
    val tag: GitTagConfig? get() = null

    /**
     * Returns a json string representation of this [ModuleConfig] instance.
     */
    fun jsonString(): String {
        return tag?.let { """{ "name": "$name", "sources": "$sources", ${it.jsonString()} }""" }
            ?: """{ "name": "$name", "sources": "$sources" }"""
    }
}

/**
 * Creates a [ModuleConfig] instance with the given [name] and optional [sources].
 */
fun ModuleConfig(name: String, sources: Path? = null, tag: GitTagConfig? = null): ModuleConfig = object : ModuleConfig {
    override val name: String = name
    override val sources: Path = sources ?: super.sources
    override val tag: GitTagConfig? = tag ?: super.tag
}
