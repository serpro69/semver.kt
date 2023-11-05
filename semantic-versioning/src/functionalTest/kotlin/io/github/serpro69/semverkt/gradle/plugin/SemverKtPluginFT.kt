package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitRepoConfig
import io.github.serpro69.semverkt.release.configuration.PojoConfiguration
import io.kotest.core.names.DuplicateTestNameMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class SemverKtPluginFT : DescribeSpec({

    assertSoftly = true
    duplicateTestNameMode = DuplicateTestNameMode.Silent

    describe("versioning from commits") {
        listOf("major", "minor", "patch", "pre release").forEach { keyword ->
            it("should set initial version via commit message with [$keyword] keyword") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[$keyword]").call()
                }
                // Act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                if (keyword == "pre release") result.output shouldContain "Calculated next version: 0.1.0-rc.1"
                else result.output shouldContain "Calculated next version: 0.1.0"
            }

            it("should set next version via commit message with [$keyword] keyword") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[$keyword]").call()
                }
                // Act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                // Assert
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

    describe("versioning from gradle properties") {
        listOf("major", "minor", "patch", "pre_release").forEach { keyword ->
            it("should set initial version via -Pincrement=$keyword") {
                val project = SemverKtTestProject()
                // Act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Pincrement=$keyword"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                if (keyword == "pre_release") result.output shouldContain "Calculated next version: 0.1.0-rc.1"
                else result.output shouldContain "Calculated next version: 0.1.0"
            }

            it("should take precedence with -Pincrement=$keyword over commit message with [major] keyword") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[major]")
                        .call() // set to major to check if lower precedence values will override
                }
                // Act
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Pincrement=$keyword"))
                // Assert
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

    describe("custom configuration") {
        context("versioning from commits with custom git message configuration") {
            val config: (dir: Path) -> PojoConfiguration = { dir ->
                val repo: GitRepoConfig = object : GitRepoConfig {
                    override val directory: Path = dir
                }
                val message: GitMessageConfig = object : GitMessageConfig {
                    override val major: String = "rojam"
                    override val minor: String = "ronim"
                    override val patch: String = "hctap"
                    override val preRelease: String = "esaeler erp"
                }
                PojoConfiguration(gitRepoConfig = repo, gitMessageConfig = message)
            }
            listOf(
                "rojam",
                "ronim",
                "hctap",
                "esaeler erp",
            ).forEach { keyword ->
                it("should set initial version via commit message with [$keyword] keyword") {
                    val project = SemverKtTestProject(configure = config)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit\n\n[$keyword]").call()
                    }
                    // Act
                    val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                    if (keyword == "esaeler erp") result.output shouldContain "Calculated next version: 0.1.0-rc.1"
                    else result.output shouldContain "Calculated next version: 0.1.0"
                }

                it("should set next version via commit message with [$keyword] keyword") {
                    val project = SemverKtTestProject(configure = config)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit\n\n[$keyword]").call()
                    }
                    // Act
                    val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    val nextVer = when (keyword) {
                        "rojam" -> "1.0.0"
                        "ronim" -> "0.2.0"
                        "hctap" -> "0.1.1"
                        "esaeler erp" -> "0.2.0-rc.1"
                        else -> "42" // shouldn't really get here
                    }
                    result.output shouldContain "Calculated next version: $nextVer"
                }
            }
        }
    }

    describe("nextVersion < latestVersion") {
        it("should NOT set version without keyword in commit message") {
            val project = SemverKtTestProject()
            // arrange
            Git.open(project.projectDir.toFile()).use {
                it.tag().setName("v0.1.0").call()
                project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                it.add().addFilepattern("text.txt").call()
                it.commit().setMessage("New commit without release message").call()
            }
            // act
            val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
            // assert
            result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
            // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
            result.output shouldContain "> Configure project :\nProject test-project version: 0.0.0"
            result.output shouldContain "> Task :tag\nNot doing anything"
            result.output shouldNotContain "Calculated next version"
        }
    }

    describe("current git HEAD with a version") {
        listOf(true, false).forEach { dryRun ->
            it("should set version to current version with increment from commit message${if (dryRun) " and dryRun" else ""}") {
                val project = SemverKtTestProject()
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n[minor]").call()
                    it.tag().setName("v0.1.0").call() // add a tag manually on latest commit
                }
                // act
                val args = if (dryRun) arrayOf("tag", "-PdryRun") else arrayOf("tag")
                val result = Builder.build(project = project, args = args)
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain "> Configure project :\nProject test-project version: 0.1.0"
                result.output shouldContain "> Task :tag\nCurrent version: 0.1.0"
                result.output shouldNotContain "Calculated next version"
            }
            it("should set version to current version with increment from gradle properties${if (dryRun) " and dryRun" else ""}") {
                val project = SemverKtTestProject()
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit").call()
                    it.tag().setName("v0.1.0").call() // add a tag manually on latest commit
                }
                // act
                val args =
                    if (dryRun) arrayOf("tag", "-PdryRun", "-Pincrement=minor") else arrayOf("tag", "-Pincrement=minor")
                val result = Builder.build(project = project, args = args)
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain "> Configure project :\nProject test-project version: 0.1.0"
                result.output shouldContain "> Task :tag\nCurrent version: 0.1.0"
                result.output shouldNotContain "Calculated next version"
            }
        }
    }

    describe("re-run 'tag' after releasing a version") {
        listOf(true, false).forEach { dryRun ->
            it("should return UP_TO_DATE with increment from commit${if (dryRun) " and dryRun" else ""}") {
                val project = SemverKtTestProject()
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[minor]").call()
                }
                Builder.build(project = project, args = arrayOf("tag")) // release a version
                // act
                val args = if (dryRun) arrayOf("tag", "-PdryRun") else arrayOf("tag")
                val result = Builder.build(project = project, args = args)
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                result.output shouldContain "> Configure project :\nProject test-project version: 0.1.0"
                result.output shouldContain "> Task :tag UP-TO-DATE"
                result.output shouldNotContain "Calculated next version"
            }
            it("should return UP_TO_DATE with increment from gradle properties${if (dryRun) " and dryRun" else ""}") {
                val project = SemverKtTestProject()
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit").call()
                }
                Builder.build(project = project, args = arrayOf("tag", "-Pincrement=minor")) // release a version
                // act
                val args =
                    if (dryRun) arrayOf("tag", "-PdryRun", "-Pincrement=minor") else arrayOf("tag", "-Pincrement=minor")
                val result = Builder.build(project = project, args = args)
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                result.output shouldContain "> Configure project :\nProject test-project version: 0.1.0"
                result.output shouldContain "> Task :tag UP-TO-DATE"
                result.output shouldNotContain "Calculated next version"
            }
        }
    }

    describe("compatible gradle version") {
        // gradle 8.x
        (0..4).forEach { minorVer ->
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
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                    result.output shouldContain "Calculated next version: 0.1.0"
                }
            }
        }
    }

    // test is broken because plugin-under-test.metadata file contains effective classpath of the plugin,
    // and hence even though we run with gradle 7.x, classpath contains kotlin 1.9.0,
    // which is the reason why gradle 7.x is incompatible in the first place
    // not sure yet how to make this work in an automated way
    describe("!incompatible gradle version") {
        // gradle 7.x
        context("gradle 7.x") {
            // disable soft assertions to fail-fast in Arrange
            assertSoftly = false

            it("should fail due to incompatible gradle version") {
                // Arrange
                // - run gradle task with default settings
                val project = SemverKtTestProject(defaultSettings = true)
                Builder.build(
                    gradleVersion = GradleVersion.version("7.6.3"), // latest 7.x version
                    project = project,
                    args = arrayOf("clean")
                    // just add some dumb assertion to verify gradle actually ran successfully
                ).also { it.task(":clean")?.outcome shouldBe TaskOutcome.UP_TO_DATE }
                // - add plugin to settings.gradle file
                project.writePluginSettings(false)
                // - add a file and commit with release keyword
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New [minor] version").call()
                }
                // Act
                val result = Builder.buildAndFail(
                    gradleVersion = GradleVersion.version("7.6.3"),
                    project = project,
                    args = arrayOf("tag", "-PdryRun")
                )
                // Assert
                result.task(":tag")?.outcome shouldBe null // settings compilation should fail
                result.output shouldNotContain "Calculated next version"
            }

            // re-enable soft assertions
            assertSoftly = false
        }
    }

    describe("multi-module project") {
        listOf(true, false).forEach { dryRun ->
            it("should set next version via commit${if (dryRun) " and dryRun" else ""}") {
                val project = SemverKtTestProject(multiModule = true)
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[minor]").call()
                }
                // act
                val args = if (dryRun) arrayOf("tag", "-PdryRun") else arrayOf("tag")
                val result = Builder.build(project = project, args = args)
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain """
                    > Task :tag
                    Calculated next version: 0.2.0

                    > Task :submodule:tag
                    Calculated next version: 0.2.0
                    ${if (!dryRun) "Tag v0.2.0 already exists in project" else ""}
                """.trimIndent().trim()
            }

            it("should set next version via gradle property${if (dryRun) " and dryRun" else ""}") {
                val project = SemverKtTestProject(multiModule = true)
                // arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit").call()
                }
                // act
                val args =
                    if (dryRun) arrayOf("tag", "-PdryRun", "-Pincrement=minor") else arrayOf("tag", "-Pincrement=minor")
                val result = Builder.build(project = project, args = args)
                // assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain """
                    > Task :tag
                    Calculated next version: 0.2.0

                    > Task :submodule:tag
                    Calculated next version: 0.2.0
                    ${if (!dryRun) "Tag v0.2.0 already exists in project" else ""}
                """.trimIndent().trim()
            }
        }
    }

    describe("setting version manually") {
        listOf("major", "minor", "patch", "pre release").forEach { keyword ->
            it("should set version via -Pversion even when commit message has [$keyword] keyword") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[$keyword]").call()
                }
                // Act
                val result = Builder.build(
                    project = project,
                    args = arrayOf("tag", "-PdryRun", "-Pversion=42.0.0")
                )
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Calculated next version: 42.0.0"
            }

            it("should set version via -Pversion even when -Pincrement=$keyword is used") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[$keyword]").call()
                }
                // Act
                val result = Builder.build(
                    project = project,
                    args = arrayOf("tag", "-PdryRun", "-Pincrement=$keyword", "-Pversion=42.0.0")
                )
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Calculated next version: 42.0.0"
            }
        }
    }
})
