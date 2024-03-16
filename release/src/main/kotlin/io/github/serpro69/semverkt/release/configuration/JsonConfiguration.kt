package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Provides access to default [Configuration] properties with optional overrides through json.
 */
class JsonConfiguration : ConfigurationProvider {
    private val json: JSONObject

    /**
     * Creates an instance of this [JsonConfiguration] from a [json] string.
     *
     * It is up to the client to perform any checks to validate the [json] string prior to calling this constructor.
     */
    constructor(json: String) : super() {
        this.json = JSONObject(json)
    }

    /**
     * Creates an instance of this [JsonConfiguration] from a [jsonFile].
     *
     * It is up to the client to perform any checks to validate the [jsonFile] prior to calling this constructor.
     */
    constructor(jsonFile: File) : super() {
        json = JSONObject(jsonFile.readText())
    }

    private fun stringOrNull(json: JSONObject, name: String): String? = getValueByPath(json, name)?.toString()

    override fun stringOrNull(name: String): String? = stringOrNull(json, name)

    override fun intOrNull(name: String): Int? {
        return when (val prop = getValueByPath(json, name)) {
            is Int -> prop
            else -> prop?.toString()?.toIntOrNull()
        }
    }

    private fun booleanOrNull(json: JSONObject, name: String): Boolean? {
        return when (val prop = getValueByPath(json, name)) {
            is Boolean -> prop
            else -> prop?.toString()?.toBoolean()
        }
    }

    override fun booleanOrNull(name: String): Boolean? = booleanOrNull(json, name)

    override fun incrementOrNull(name: String): Increment? {
        return when (val prop = getValueByPath(json, name)) {
            is Increment -> prop
            else -> prop?.toString()?.let {
                when (val inc = Increment.getByName(it)) {
                    Increment.MAJOR, Increment.MINOR, Increment.PATCH -> inc
                    else -> throw IllegalArgumentException("Illegal Increment configuration value: '$name'")
                }
            }
        }
    }

    override fun cleanRuleOrNull(name: String): CleanRule? {
        return when (val prop = getValueByPath(json, name)) {
            is CleanRule -> prop
            else -> prop?.toString()?.let { CleanRule.getByName(it) }
        }
    }

    override fun semverOrNull(name: String): Semver? {
        return when (val prop = getValueByPath(json, name)) {
            is Semver -> prop
            else -> prop?.toString()?.let { Semver(it) }
        }
    }

    private fun pathOrNull(json: JSONObject, name: String): Path? {
        return when (val prop = getValueByPath(json, name)) {
            is Path -> prop
            else -> prop?.toString()?.let { Path(it) }
        }
    }

    override fun pathOrNull(name: String): Path? = pathOrNull(json, name)

    private fun tagPrefixOrNull(json: JSONObject, name: String): TagPrefix? {
        return when (val prop = getValueByPath(json, name)) {
            is TagPrefix -> prop
            else -> prop?.toString()?.let { TagPrefix(it) }
        }
    }

    override fun tagPrefixOrNull(name: String): TagPrefix? = tagPrefixOrNull(json, name)

    override fun listOfModules(name: String): List<ModuleConfig> {
        val array = getValueByPath(json, "monorepo") ?: return emptyList()
        if (array !is JSONArray) throw IllegalArgumentException("Object with '$name' name is not a json array")
        return array.map {
            val j = JSONObject(it.toString())
            object : ModuleConfig {
                override val path: String = requireNotNull(j.optString("path")) { "Module path can't be null" }
                override val sources: Path = pathOrNull(j, "sources") ?: super.sources
                override val tag: GitTagConfig? = j.opt("tag")?.let { t ->
                    val tag = JSONObject(t.toString())
                    object : GitTagConfig {
                        override val prefix: TagPrefix = tagPrefixOrNull(tag, "prefix") ?: super.prefix
                        override val separator: String = stringOrNull(tag, "separator") ?: super.separator
                        override val useBranches: Boolean = booleanOrNull(tag, "useBranches") ?: super.useBranches
                    }
                }

                init {
                    if (path.isBlank()) {
                        throw IllegalArgumentException("Module path cannot be blank")
                    }
                }
            }
        }
    }

    private tailrec fun getValueByPath(json: JSONObject, path: String): Any? {
        val dotIndex = path.indexOf(".")
        if (dotIndex == -1) return json.opt(path)
        // Split the path into the next key and the remaining path
        val (key, remainder) = path.substring(0, dotIndex) to path.substring(dotIndex + 1)
        // If the next level object is a JSONObject, recurse with the remaining path
        return when (val next = json.opt(key)) {
            is JSONObject -> getValueByPath(next, remainder)
            // If next level is not a JSONObject, the path is invalid
            else -> null
        }
    }
}
