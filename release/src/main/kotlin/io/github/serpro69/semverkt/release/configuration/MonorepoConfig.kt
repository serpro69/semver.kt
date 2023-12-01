package io.github.serpro69.semverkt.release.configuration

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Monorepo module configuration enables individual versioning of multi-modules in a mono-repo.
 *
 * @property modules mono-repo modules
 */
interface MonorepoConfig{
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
 * @property name    module directory name (e.g. `core` for `./core` submodule in the root of the mono-repo)
 * @property sources path to track changes for the module, relative to the module dir [name].
 * Defaults to module directory.
 */
interface ModuleConfig {
    val name: String
    val sources: Path get() = Path(".")

    fun jsonString(): String {
        return """
            { "name": "$name", "sources": "$sources" }
        """.trimIndent()
    }
}

fun ModuleConfig(name: String, sources: Path? = null): ModuleConfig = object : ModuleConfig {
    override val name: String = name
    override val sources: Path = sources ?: super.sources
}
