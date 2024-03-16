package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import org.json.JSONObject
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Provides access to default [Configuration] properties with optional overrides through json.
 */
class JsonConfiguration : ConfigurationProvider {
    val json: JSONObject

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

    override fun stringOrNull(name: String): String? = getValueByPath(json, name)?.toString()

    override fun intOrNull(name: String): Int? {
        return when (val prop = getValueByPath(json, name)) {
            is Int -> prop
            else -> prop?.toString()?.toIntOrNull()
        }
    }

    override fun booleanOrNull(name: String): Boolean? {
        return when (val prop = getValueByPath(json, name)) {
            is Boolean -> prop
            else -> prop?.toString()?.toBoolean()
        }
    }

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

    override fun pathOrNull(name: String): Path? {
        return when (val prop = getValueByPath(json, name)) {
            is Path -> prop
            else -> prop?.toString()?.let { Path(it) }
        }
    }

    override fun tagPrefixOrNull(name: String): TagPrefix? {
        return when (val prop = getValueByPath(json, name)) {
            is TagPrefix -> prop
            else -> prop?.toString()?.let { TagPrefix(it) }
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
