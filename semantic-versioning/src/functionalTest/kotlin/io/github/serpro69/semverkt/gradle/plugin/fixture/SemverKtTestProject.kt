package io.github.serpro69.semverkt.gradle.plugin.fixture

import io.github.serpro69.semverkt.release.configuration.Configuration
import org.eclipse.jgit.api.Git
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class SemverKtTestProject(
    defaultSettings: Boolean = false,
    multiModule: Boolean = false,
    monorepo: Boolean = multiModule,
    multiTag: Boolean = false,
    useSnapshots: Boolean = false,
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
            rootProject.name = "test-project"

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
      
            rootProject.name = "test-project"

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
                    ${if (monorepo && multiModule) 
                    """
                    // different ways to add module configuration
                    //modules.add(ModuleConfig(":foo", Paths.get(".")))
                    module(":foo") {
                        sources = Paths.get(".")
                        ${if (multiTag) "tag { prefix = TagPrefix(\"foo-v\") }" else "" }
                    }
                    module(":bar") {
                        sources = Paths.get(".")
                        ${if (multiTag) "tag { prefix = TagPrefix(\"bar-v\") }" else "" }
                    }
                    module(":baz") {
                        sources = Paths.get(".")
                        ${if (multiTag) "tag { prefix = TagPrefix(\"baz-v\") }" else "" }
                    }
                    """
                    else ""}
                }
            }

            ${if (multiModule) "include(\":core\", \":foo\", \":bar\", \":baz\")" else ""}
            """.trimIndent()
        )
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
