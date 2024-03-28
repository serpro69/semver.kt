package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.kotest.core.names.DuplicateTestNameMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class GradleCompatibilityFT : DescribeSpec({

    assertSoftly = true
    duplicateTestNameMode = DuplicateTestNameMode.Silent

    describe("compatible gradle version") {
        context("gradle 7.5") {
            it("should be compatible") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New [minor] version").call()
                }
                // Act
                val result = Builder.build(
                    gradleVersion = GradleVersion.version("7.5"),
                    project = project,
                    args = arrayOf("tag", "-PdryRun")
                )
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain "Calculated next version: 0.1.0"
            }
        }
        // gradle 8.x
        (0..5).forEach { minorVer ->
            context("gradle 8.$minorVer") {
                it("should be compatible") {
                    val project = SemverKtTestProject()
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New [minor] version").call()
                    }
                    // Act
                    val result = Builder.build(
                        gradleVersion = GradleVersion.version("8.$minorVer"),
                        project = project,
                        args = arrayOf("tag", "-PdryRun")
                    )
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                    // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                    result.output shouldContain "Calculated next version: 0.1.0"
                }
            }
        }
    }

    // Test is broken because the test is executed with gradle8,
    // and hence the attribute org.gradle.plugin.api-version is set to the gradle runtime,
    // and not the version of gradle build runner.
    // Not sure yet how to make this work
    xdescribe("incompatible gradle version") {
        // latest incompatible gradle version
        context("gradle 7.4") {
            // disable soft assertions to fail-fast in Arrange
            assertSoftly = false

            it("should fail due to incompatible gradle version") {
                // Arrange
                // - run gradle task with default settings
                val project = SemverKtTestProject(defaultSettings = true)
                Builder.build(
                    gradleVersion = GradleVersion.version("7.4"),
                    project = project,
                    args = arrayOf("clean")
                    // just add some dumb assertion to verify gradle actually ran successfully
                ).also { it.task(":clean")?.outcome shouldBe TaskOutcome.UP_TO_DATE }
                // - add plugin to settings.gradle file
                project.writePluginSettings(multiModule = false, useSnapshots = false)
                // - add a file and commit with release keyword
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New [minor] version").call()
                }
                // Act
                val result = Builder.buildAndFail(
                    gradleVersion = GradleVersion.version("7.4"),
                    project = project,
                    args = arrayOf("tag", "-PdryRun")
                )
                // Assert
                result.task(":tag")?.outcome shouldBe null // settings compilation should fail
                result.output shouldContain "No matching variant of io.github.serpro69:semantic-versioning:0.0.0 was found"
                result.output shouldNotContain "Calculated next version"
            }

            // re-enable soft assertions
            assertSoftly = false
        }
    }
})

