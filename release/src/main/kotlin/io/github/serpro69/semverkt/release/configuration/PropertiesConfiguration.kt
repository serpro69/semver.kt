package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path

/**
 * Provides access to default [Configuration] with optional overrides through java [Properties]
 *
 * NB! This class does not support [Configuration.monorepo] config property because it's a complex type.
 */
class PropertiesConfiguration : ConfigurationProvider {
    private val props: Properties

    /**
     * Creates an instance of this [PropertiesConfiguration] from a [props] instance.
     */
    constructor(props: Properties) : super() {
        this.props = props
    }

    /**
     * Creates an instance of this [PropertiesConfiguration] from a [props] map.
     */
    constructor(props: Map<*, *>) : super() {
        this.props = Properties().also { it.putAll(props) }
    }

    override fun stringOrNull(name: String): String? = props.getProperty(name)?.toString()

    override fun intOrNull(name: String): Int? {
        return when (val prop = props[name]) {
            is Int -> prop
            else -> prop?.toString()?.toIntOrNull()
        }
    }

    override fun booleanOrNull(name: String): Boolean? {
        return when (val prop = props[name]) {
            is Boolean -> prop
            else -> prop?.toString()?.toBoolean()
        }
    }

    override fun incrementOrNull(name: String): Increment? {
        return when (val prop = props[name]) {
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
        return when (val prop = props[name]) {
            is CleanRule -> prop
            else -> prop?.toString()?.let { CleanRule.getByName(it) }
        }
    }

    override fun semverOrNull(name: String): Semver? {
        return when (val prop = props[name]) {
            is Semver -> prop
            else -> prop?.toString()?.let { Semver(it) }
        }
    }

    override fun pathOrNull(name: String): Path? {
        return when (val prop = props[name]) {
            is Path -> prop
            else -> prop?.toString()?.let { Path(it) }
        }
    }

    override fun tagPrefixOrNull(name: String): TagPrefix? {
        return when (val prop = props[name]) {
            is TagPrefix -> prop
            else -> prop?.toString()?.let { TagPrefix(it) }
        }
    }

    /**
     * This function is not supported in [PropertiesConfiguration] provider and will always default to an empty list,
     * returning the property value of the super class for list-typed properties.
     */
    override fun listOfModules(name: String): List<ModuleConfig> = emptyList()
}
