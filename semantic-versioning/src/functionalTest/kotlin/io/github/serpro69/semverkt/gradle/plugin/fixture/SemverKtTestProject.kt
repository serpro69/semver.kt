package io.github.serpro69.semverkt.gradle.plugin.fixture

import io.github.serpro69.semverkt.release.configuration.Configuration
import io.github.serpro69.semverkt.release.configuration.ModuleConfig
import org.eclipse.jgit.api.Git
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendLines
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.readLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

class SemverKtTestProject(
    private val defaultSettings: Boolean = false,
    private val multiModule: Boolean = false,
    private val monorepo: Boolean = multiModule,
    private val multiTag: Boolean = false,
    private val useSnapshots: Boolean = false,
) : AbstractProject() {

    private val gradlePropertiesFile = projectDir.resolve("gradle.properties")
    private val settingsFile = projectDir.resolve("settings.gradle.kts")
    private val buildFile = projectDir.resolve("build.gradle.kts")
    private val configFile = projectDir.resolve("semantic-versioning.json")

    constructor(
        defaultSettings: Boolean = false,
        multiModule: Boolean = false,
        monorepo: Boolean = multiModule,
        multiTag: Boolean = multiModule,
        useSnapshots: Boolean = false,
        configure: (dir: Path) -> Configuration,
    ) : this(
        defaultSettings = defaultSettings,
        multiModule = multiModule,
        monorepo = monorepo,
        multiTag = multiTag,
        useSnapshots = useSnapshots
    ) {
        configFile.writeText(configure(projectDir).jsonString())
    }

    init {
        gradlePropertiesFile.writeText(
            """
            org.gradle.jvmargs=-Dfile.encoding=UTF-8
            version=0.0.0
            """.trimIndent()
        )

        if (defaultSettings) settingsFile.writeText(
            """
            rootProject.name = "$name"

            ${if (multiModule) "include(\":core\", \":foo\", \":bar\", \":baz\")" else ""}
            """.trimIndent()
        )
        else writePluginSettings(
            multiModule = multiModule,
            useSnapshots = useSnapshots,
            monorepo = monorepo,
            multiTag = multiTag
        )

        // Apply our plugin
        buildFile.writeBuildFile()

        if (multiModule) listOf("core", "foo", "bar", "baz").forEach { subModule ->
            projectDir.resolve(subModule).createDirectories().also {
                it.resolve("build.gradle.kts").writeText("""
                plugins {
                }
                """.trimIndent())
            }
        }

        Git.open(projectDir.toFile()).use {
            it.add().addFilepattern(".").call()
            it.commit().setMessage("add project files").call()
        }
    }

    fun writePluginSettings(
        multiModule: Boolean,
        useSnapshots: Boolean,
        monorepo: Boolean = multiModule,
        multiTag: Boolean = multiModule
    ) {
        settingsFile.writeText(
            """
            import io.github.serpro69.semverkt.gradle.plugin.SemverPluginExtension
            import io.github.serpro69.semverkt.release.configuration.ModuleConfig
            import io.github.serpro69.semverkt.release.configuration.TagPrefix
            import java.nio.file.Paths

            plugins {
                id("io.github.serpro69.semantic-versioning")
            }
      
            rootProject.name = "$name"

            settings.extensions.configure<SemverPluginExtension>("semantic-versioning") {
                git {
                    repo {
                        directory = Paths.get("${projectDir.absolutePathString()}")
                    }
                }
                version {
                    ${if (useSnapshots) "useSnapshots = true" else ""}
                }
                monorepo {
                    sources = Paths.get("src")
                    ${if (monorepo && multiModule) 
                    """
                    // different ways to add module configuration
                    //modules.add(ModuleConfig(":foo", Paths.get(".")))
                    module(":foo") {
                        sources = Paths.get("foosrc")
                        ${if (multiTag) "tag { prefix = TagPrefix(\"foo-v\") }" else "" }
                    }
                    module(":bar") {
                        sources = Paths.get("barsrc")
                        ${if (multiTag) "tag { prefix = TagPrefix(\"bar-v\") }" else "" }
                    }
                    module(":baz") {
                        sources = Paths.get("bazsrc")
                        ${if (multiTag) "tag { prefix = TagPrefix(\"baz-v\") }" else "" }
                    }
                    """ else ""}
                }
            }

            ${if (multiModule) "include(\":core\", \":foo\", \":bar\", \":baz\")" else ""}
            """.trimIndent()
        )
    }

    fun add(module: ModuleConfig) {
        if (multiModule && monorepo) {
            val lines = settingsFile.readLines()
            val first = lines.dropLast(4)
            val last = lines.takeLast(4)
            val new = """
        |        module(":${module.path}") {
        |            sources = Paths.get(".")
        |            tag { prefix = TagPrefix("${module.path}-v") }
        |        }
            """.trimMargin("|").split("\n")

            settingsFile.writeLines(first)
                .appendLines(new)
                .appendLines(last)
                .appendText("\ninclude(\":${module.path}\")")

            projectDir.resolve(module.path)
                .createDirectories()
                .also {
                    it.resolve("build.gradle.kts").writeText("""
                        plugins {
                        }
                    """.trimIndent())
                }
        }
    }

    private fun Path.writeBuildFile() {
        writeText(
            """
            plugins {
                java
            }
      
            println("Project ${'$'}{project.name} version: ${'$'}{project.version}")

            tasks.create("ft", Test::class.java) {
                doFirst {
                    println("Hello")
                }
            }
            """.trimIndent()
        )
    }
}
