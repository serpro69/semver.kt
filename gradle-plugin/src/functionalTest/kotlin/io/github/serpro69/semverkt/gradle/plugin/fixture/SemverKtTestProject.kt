package io.github.serpro69.semverkt.gradle.plugin.fixture

import org.eclipse.jgit.api.Git
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

class SemverKtTestProject : AbstractProject() {

    private val gradlePropertiesFile = projectDir.resolve("gradle.properties")
    private val settingsFile = projectDir.resolve("settings.gradle.kts")
    private val buildFile = projectDir.resolve("build.gradle.kts")

    init {
//        projectDir.resolve("abc.txt").writeText("hello world")
//
//        projectDir.resolve("sub").createDirectories()
//            .also {
//                it.resolve("build.gradle.kts")
//                .writeText("""
//                    plugins {
//                    }
//                """.trimIndent())
//
//                it.resolve("abc.txt")
//                    .writeText("another one")
//            }
        // Yes, this is independent of our plugin project's properties file
        gradlePropertiesFile.writeText(
            """
            org.gradle.jvmargs=-Dfile.encoding=UTF-8
            """.trimIndent()
        )

        // Yes, our project under test can use build scans. It's a real project!
        settingsFile.writeText(
            """
            import java.nio.file.Paths
            import io.github.serpro69.semverkt.gradle.plugin.SemverPluginExtension

            plugins {
                id("io.github.serpro69.semver-release")
            }
      
            rootProject.name = "test-project"

            settings.extensions.configure<SemverPluginExtension>("semver-release") {
                git {
                    repo {
                        directory = Paths.get("${projectDir.absolutePathString()}")
                    }
                }
            }
            """.trimIndent()
        )

        // Apply our plugin
        buildFile.writeText(
            """
            plugins {
                java
            }
      
            println("Project version: ${'$'}{project.version}")

            tasks.create("ft", Test::class.java) {
                doFirst {
                    println("Hello")
                }
            }
            """.trimIndent()
        )

        Git.open(projectDir.toFile()).use {
            it.add().addFilepattern(".").call()
            it.commit().setMessage("add project files").call()
        }
    }
}
