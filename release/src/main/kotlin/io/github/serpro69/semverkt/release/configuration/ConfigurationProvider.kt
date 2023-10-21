@file:Suppress("unused")

package io.github.serpro69.semverkt.release.configuration

import dev.nohus.autokonfig.AutoKonfig
import dev.nohus.autokonfig.AutoKonfigException
import dev.nohus.autokonfig.types.getBoolean
import dev.nohus.autokonfig.types.getInt
import dev.nohus.autokonfig.types.getString
import dev.nohus.autokonfig.withConfig
import dev.nohus.autokonfig.withProperties
import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createTempFile

/**
 * Provides access to default [Configuration] properties without the possibility to override any of them.
 */
class DefaultConfiguration : ConfigurationProvider(AutoKonfig())

/**
 * Provides access to default [Configuration] properties with optional overrides through [Properties].
 */
class PropertiesConfiguration : ConfigurationProvider {

    /**
     * Creates an instance of this [PropertiesConfiguration] from the given [properties].
     */
    constructor(properties: Properties) : super(AutoKonfig().withProperties(properties))

    /**
     * Creates an instance of this [PropertiesConfiguration] from a [propertiesFile].
     *
     * It is up to the client to perform any checks to validate the [propertiesFile] prior to calling this constructor.
     */
    constructor(propertiesFile: File) : super(AutoKonfig().withConfig(propertiesFile))
}

/**
 * Provides access to default [Configuration] properties with optional overrides through json.
 */
class JsonConfiguration : ConfigurationProvider {

    /**
     * Creates an instance of this [JsonConfiguration] from a [json] string.
     *
     * It is up to the client to perform any checks to validate the [json] string prior to calling this constructor.
     */
    constructor(json: String) : super(
        with(createTempFile().toFile().also { it.writeText(json) }) {
            AutoKonfig().withConfig(this).also { delete() }
        }
    )

    /**
     * Creates an instance of this [JsonConfiguration] from a [jsonFile].
     *
     * It is up to the client to perform any checks to validate the [jsonFile] prior to calling this constructor.
     */
    constructor(jsonFile: File) : super(AutoKonfig().withConfig(jsonFile))
}

/**
 * Abstract class for providing [Configuration]s through [autoConfig].
 */
abstract class ConfigurationProvider internal constructor(private val autoConfig: AutoKonfig) : Configuration {

    private val gitRepoConfig = object : GitRepoConfig {
        override val directory: Path = autoConfig.propertyOrNull("git.repo.directory") ?: super.directory
        override val remoteName: String = autoConfig.propertyOrNull("git.repo.remoteName") ?: super.remoteName
    }

    private val gitTagConfig = object : GitTagConfig {
        override val prefix: String = autoConfig.propertyOrNull("git.tag.prefix") ?: super.prefix
        override val separator: String = autoConfig.propertyOrNull("git.tag.separator") ?: super.separator
        override val useBranches: Boolean = autoConfig.propertyOrNull("git.tag.useBranches") ?: super.useBranches
    }

    private val gitMessageConfig = object : GitMessageConfig {
        override val major: String = autoConfig.propertyOrNull("git.message.major") ?: super.major
        override val minor: String = autoConfig.propertyOrNull("git.message.minor") ?: super.minor
        override val patch: String = autoConfig.propertyOrNull("git.message.patch") ?: super.patch
        override val preRelease: String = autoConfig.propertyOrNull("git.message.preRelease") ?: super.preRelease
        override val ignoreCase: Boolean = autoConfig.propertyOrNull("git.message.ignoreCase") ?: super.ignoreCase
    }

    override val git: GitConfig = object : GitConfig {
        override val repo: GitRepoConfig = gitRepoConfig
        override val tag: GitTagConfig = gitTagConfig
        override val message: GitMessageConfig = gitMessageConfig
    }

    override val version: VersionConfig = object : VersionConfig {
        override val initialVersion: Semver = autoConfig.propertyOrNull("version.initialVersion") ?: super.initialVersion
        override val defaultIncrement: Increment = autoConfig.propertyOrNull("version.defaultIncrement") ?: super.defaultIncrement
        override val preReleaseId: String = autoConfig.propertyOrNull("version.preReleaseId") ?: super.preReleaseId
        override val initialPreRelease: Int = autoConfig.propertyOrNull("version.initialPreRelease") ?: super.initialPreRelease
        override val snapshotSuffix: String = autoConfig.propertyOrNull("version.snapshotSuffix") ?: super.snapshotSuffix
    }

    /**
     * Returns a property of [T] type from this [AutoKonfig] receiver by the property name [name]
     * or throws an [AutoKonfigException] if the property is not found.
     */
    internal inline fun <reified T : Any> AutoKonfig.property(name: String): T {
        return try {
            when (T::class) {
                String::class -> getString(name) as T
                Int::class -> getInt(name) as T
                Boolean::class -> getBoolean(name) as T
                Path::class -> Path(getString(name)) as T
                Semver::class -> Semver(getString(name)) as T
                Increment::class -> {
                    when (val inc = Increment.getByName(getString(name))) {
                        Increment.MAJOR, Increment.MINOR, Increment.PATCH -> inc as T
                        else -> throw IllegalArgumentException("Illegal Increment configuration value: '$name'")
                    }
                }
                else -> throw IllegalArgumentException("Unsupported property type ${T::class}")
            }
        } catch (e: AutoKonfigException) {
            throw e
        }
    }

    /**
     * Returns a property of [T] type from this [AutoKonfig] receiver by the property name [name]
     * or `null` if the property is not found
     */
    internal inline fun <reified T : Any> AutoKonfig.propertyOrNull(name: String): T? {
        return try {
            property(name)
        } catch (e: AutoKonfigException) {
            null
        }
    }
}
