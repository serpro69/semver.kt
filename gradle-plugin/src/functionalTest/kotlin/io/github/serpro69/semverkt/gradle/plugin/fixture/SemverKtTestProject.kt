package io.github.serpro69.semverkt.gradle.plugin.fixture

import kotlin.io.path.writeText

class SemverKtTestProject : AbstractProject() {

    private val gradlePropertiesFile = projectDir.resolve("gradle.properties")
    private val settingsFile = projectDir.resolve("settings.gradle")
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
            plugins {
//                id 'com.gradle.enterprise' version '3.8.1'
                id 'io.github.serpro69.gradle.semver-release'
            }
      
//            gradleEnterprise {
//                buildScan {
//                   publishAlways()
//                   termsOfServiceUrl = 'https://gradle.com/terms-of-service'
//                   termsOfServiceAgree = 'yes'
//                }
//            }
      
            rootProject.name = 'test-project'
//            include 'sub'
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
    }
}
