@file:Suppress("unused")

package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Abstract class for providing access to [Configuration] properties via dot-notation.
 *
 * The implementation class needs to ensure all required [Configuration] properties and their siblings
 * can be resolved via dot-notation using property names via overrides of `typeOrNull` methods, e.g. [stringOrNull],
 * [booleanOrNull], [pathOrNull] and others as required.
 * Unsupported methods can simply return a null value, which will result in property always having a default from [Configuration].
 *
 * All properties of [Configuration] in this [ConfigurationProvider] are therefore considered optional.
 * Implementation classes may choose to only support a subset of [Configuration] properties
 * via implementation of `typeOrNull` functions and use defaults for the rest.
 *
 * For example, if the implementation wants to make use of [GitRepoConfig.directory] property,
 * the provider implementation should be able to resolve `pathOrNull("git.repo.directory")` property value.
 *
 * The above example was formed using the following chain:
 * 1. [Configuration.git] -> `git` property name with [GitConfig] type
 * 2. [GitConfig.repo] -> `repo` property name with [GitRepoConfig] type
 * 3. [GitRepoConfig.directory] -> `directory` property name with [Path] type, which needs to implement [pathOrNull].
 *
 * See [JsonConfiguration] provider implementation as an example of implementing a config provider
 * for json-based [Configuration] and a [DefaultConfiguration] as an example of using defaults.
 */
abstract class ConfigurationProvider : Configuration {
    protected val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /*
     * we need to eval lazy or convert access to getter,
     * otherwise initialization will fail for implementation classes with an NPE
     * since this class and it's properties will be initialized before the implementation,
     * which might use a property like 'json' inside 'propertyOrNull',
     * and at that time 'json' will be null
     */
    private val gitRepoConfig by lazy {
        object : GitRepoConfig {
            override val directory: Path = propertyOrNull("git.repo.directory") ?: super.directory
            override val remoteName: String = propertyOrNull("git.repo.remoteName") ?: super.remoteName
            override val cleanRule: CleanRule = propertyOrNull("git.repo.cleanRule")
                ?: super.cleanRule
        }
    }

    private val gitTagConfig by lazy {
        object : GitTagConfig {
            override val prefix: TagPrefix = propertyOrNull("git.tag.prefix") ?: super.prefix
            override val separator: String = propertyOrNull("git.tag.separator") ?: super.separator
            override val useBranches: Boolean = propertyOrNull("git.tag.useBranches") ?: super.useBranches
        }
    }

    private val gitMessageConfig by lazy {
        object : GitMessageConfig {
            override val major: String = propertyOrNull("git.message.major") ?: super.major
            override val minor: String = propertyOrNull("git.message.minor") ?: super.minor
            override val patch: String = propertyOrNull("git.message.patch") ?: super.patch
            override val preRelease: String = propertyOrNull("git.message.preRelease") ?: super.preRelease
            override val ignoreCase: Boolean = propertyOrNull("git.message.ignoreCase") ?: super.ignoreCase
        }
    }

    final override val git: GitConfig by lazy {
        object : GitConfig {
            override val repo: GitRepoConfig = gitRepoConfig
            override val tag: GitTagConfig = gitTagConfig
            override val message: GitMessageConfig = gitMessageConfig
        }
    }

    final override val version: VersionConfig by lazy {
        object : VersionConfig {
            override val initialVersion: Semver = propertyOrNull("version.initialVersion") ?: super.initialVersion
            override val placeholderVersion: Semver = propertyOrNull("version.placeholderVersion")
                ?: super.placeholderVersion
            override val defaultIncrement: Increment = propertyOrNull("version.defaultIncrement")
                ?: super.defaultIncrement
            override val preReleaseId: String = propertyOrNull("version.preReleaseId") ?: super.preReleaseId
            override val initialPreRelease: Int = propertyOrNull("version.initialPreRelease")
                ?: super.initialPreRelease
            override val snapshotSuffix: String = propertyOrNull("version.snapshotSuffix") ?: super.snapshotSuffix
        }
    }

    final override val monorepo: MonorepoConfig by lazy {
        object : MonorepoConfig {
            override val modules: List<ModuleConfig> = listOfModules("monorepo").ifEmpty { super.modules }
        }
    }

    /**
     * Returns a property of [T] type from this [ConfigurationProvider] by the property name [name]
     * or `null` if the property is not found
     */
    protected inline fun <reified T : Any> propertyOrNull(name: String): T? {
        return when (val t = T::class) {
            String::class -> stringOrNull(name) as T?
            Int::class -> intOrNull(name) as T?
            Boolean::class -> booleanOrNull(name) as T?
            Path::class -> pathOrNull(name) as T?
            Semver::class -> semverOrNull(name) as T?
            CleanRule::class -> cleanRuleOrNull(name) as T?
            TagPrefix::class -> tagPrefixOrNull(name) as T?
            Increment::class -> incrementOrNull(name) as T?
            else -> {
                log.warn("Unsupported property type $t")
                null
            }
        }
    }

    /**
     * Returns a property value of string type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun stringOrNull(name: String): String? = null

    /**
     * Returns a property value of int type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun intOrNull(name: String): Int? = null

    /**
     * Returns a property value of boolean type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun booleanOrNull(name: String): Boolean? = null

    /**
     * Returns a property value of [Increment] type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun incrementOrNull(name: String): Increment? = null

    /**
     * Returns a property value of [CleanRule] type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun cleanRuleOrNull(name: String): CleanRule? = null

    /**
     * Returns a property value of [Semver] type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun semverOrNull(name: String): Semver? = null

    /**
     * Returns a property value of [Path] type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun pathOrNull(name: String): Path? = null

    /**
     * Returns a property value of [TagPrefix] type by the property name [name],
     * or `null` if the property is not found.
     */
    open fun tagPrefixOrNull(name: String): TagPrefix? = null

    /**
     * Returns a [List] of [ModuleConfig]s from the property [name],
     * or emptyList if the property is not found in the configuration.
     */
    open fun listOfModules(name: String): List<ModuleConfig> = emptyList()
}
