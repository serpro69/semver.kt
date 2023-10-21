package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class SemverKtPluginFT : DescribeSpec({

    assertSoftly = true

    listOf("major", "minor", "patch", "pre release").forEach { keyword ->
        describe("versioning from commits") {
            it("should set initial version via commit message with [$keyword] keyword") {
                val project = SemverKtTestProject()
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[$keyword]").call()
                }
                // act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                if (keyword == "pre release") result.output shouldContain "Calculated next version: 0.1.0-rc.1"
                else result.output shouldContain "Calculated next version: 0.1.0"
            }

            it("should set next version via commit message with [$keyword] keyword") {
                val project = SemverKtTestProject()
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[$keyword]").call()
                }
                // act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                val nextVer = when (keyword) {
                    "major" -> "1.0.0"
                    "minor" -> "0.2.0"
                    "patch" -> "0.1.1"
                    "pre release" -> "0.2.0-rc.1"
                    else -> "42" // shouldn't really get here
                }
                result.output shouldContain "Calculated next version: $nextVer"
            }
        }
    }

    listOf("major", "minor", "patch", "pre_release").forEach { keyword ->
        describe("versioning from gradle properties") {
            it("should set initial version via -Pincrement=$keyword") {
                val project = SemverKtTestProject()
                // act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Pincrement=$keyword"))
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                if (keyword == "pre_release") result.output shouldContain "Calculated next version: 0.1.0-rc.1"
                else result.output shouldContain "Calculated next version: 0.1.0"
            }

            it("should take precedence with -Pincrement=$keyword over commit message with [major] keyword") {
                val project = SemverKtTestProject()
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[major]")
                        .call() // set to major to check if lower precedence values will override
                }
                // act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Pincrement=$keyword"))
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                val nextVer = when (keyword) {
                    "major" -> "1.0.0"
                    "minor" -> "0.2.0"
                    "patch" -> "0.1.1"
                    "pre_release" -> "0.2.0-rc.1"
                    else -> "42" // shouldn't really get here
                }
                result.output shouldContain "Calculated next version: $nextVer"
            }
        }
    }
})
