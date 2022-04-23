package io.github.serpro69.semverkt.release.configuration

import dev.nohus.autokonfig.AutoKonfig
import dev.nohus.autokonfig.AutoKonfigException
import dev.nohus.autokonfig.types.getBoolean
import dev.nohus.autokonfig.types.getInt
import dev.nohus.autokonfig.types.getString
import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Provides access properties for automated releases configuration.
 *
 * @property git git configuration
 */
interface ConfigurationProvider {
    val git: GitConfig
    val version: VersionConfig
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