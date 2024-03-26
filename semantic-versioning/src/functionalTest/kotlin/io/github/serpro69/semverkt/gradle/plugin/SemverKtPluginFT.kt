package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.kfaker.faker
import io.github.serpro69.semverkt.gradle.plugin.fixture.AbstractProject
import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.github.serpro69.semverkt.gradle.plugin.util.DryRun
import io.github.serpro69.semverkt.gradle.plugin.util.ReleaseFromCommit
import io.github.serpro69.semverkt.gradle.plugin.util.ReleaseFromCommit.Companion.major
import io.github.serpro69.semverkt.gradle.plugin.util.ReleaseFromCommit.Companion.minor
import io.github.serpro69.semverkt.gradle.plugin.util.ReleaseFromCommit.Companion.patch
import io.github.serpro69.semverkt.gradle.plugin.util.ReleaseFromCommit.Companion.preRelease
import io.github.serpro69.semverkt.gradle.plugin.util.UpToDate
import io.github.serpro69.semverkt.gradle.plugin.util.fromCommit
import io.github.serpro69.semverkt.gradle.plugin.util.fromProperty
import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.release.configuration.CleanRule
import io.github.serpro69.semverkt.release.configuration.GitMessageConfig
import io.github.serpro69.semverkt.release.configuration.GitRepoConfig
import io.github.serpro69.semverkt.release.configuration.GitTagConfig
import io.github.serpro69.semverkt.release.configuration.ModuleConfig
import io.github.serpro69.semverkt.release.configuration.PojoConfiguration
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.spec.Semver
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

    DryRun.forEach { dryRun ->
        describe("versioning${testName(fromCommit, dryRun)}") {
            listOf(
                "major" to major,
                "minor" to minor,
                "patch" to patch,
                "pre release" to preRelease
            ).forEach { (keyword, r) ->
                it("should set initial version via commit message with [$keyword] keyword") {
                    val project = SemverKtTestProject()
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit\n\n[$keyword]").call()
                    }
                    // Act
                    val result = fromCommit.tag(project)(r)(dryRun)
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                    val v = if (keyword == "pre release") Semver("0.1.0-rc.1") else Semver("0.1.0")
                    result.output shouldContain expectedConfigure(v, emptyList())
                    result.output shouldContain expectedTag(project.name, v, dryRun, UpToDate.FALSE, false)
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
                    val result = fromCommit.tag(project)(r)(dryRun)
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    val nextVer = when (keyword) {
                        "major" -> "1.0.0"
                        "minor" -> "0.2.0"
                        "patch" -> "0.1.1"
                        "pre release" -> "0.2.0-rc.1"
                        else -> "42" // shouldn't really get here
                    }
                    result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                    result.output shouldContain expectedTag(
                        project.name,
                        Semver(nextVer),
                        dryRun,
                        UpToDate.FALSE,
                        false
                    )
                }
            }
        }

        describe("versioning${testName(fromProperty, dryRun)}") {
            listOf(
                "major" to major,
                "minor" to minor,
                "patch" to patch,
                "pre_release" to preRelease
            ).forEach { (inc, r) ->
                it("should set initial version via -Prelease -Pincrement=$inc") {
                    val project = SemverKtTestProject()
                    // Act
                    val result = fromProperty.tag(project)(r)(dryRun)
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                    val nextVer = if (inc == "pre_release") "0.1.0-rc.1" else "0.1.0"
                    result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                    result.output shouldContain expectedTag(
                        project.name,
                        Semver(nextVer),
                        dryRun,
                        UpToDate.FALSE,
                        false
                    )
                }

                it("should take precedence with -Prelease -Pincrement=$inc over commit message with [major] keyword") {
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
                    val result = fromProperty.tag(project)(r)(dryRun)
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    val nextVer = when (inc) {
                        "major" -> "1.0.0"
                        "minor" -> "0.2.0"
                        "patch" -> "0.1.1"
                        "pre_release" -> "0.2.0-rc.1"
                        else -> "42" // shouldn't really get here
                    }
                    result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                    result.output shouldContain expectedTag(
                        project.name,
                        Semver(nextVer),
                        dryRun,
                        UpToDate.FALSE,
                        false
                    )
                }

                it("should take precedence with commit message keyword over -Pincrement=$inc when -Prelease is NOT set") {
                    val project = SemverKtTestProject()
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit\n\n[patch]")
                            .call() // set to patch to check if lower precedence value will override the property
                    }
                    // Act
                    val args = if (!dryRun) arrayOf("tag", "-Pincrement=$inc") else arrayOf(
                        "tag",
                        "-PdryRun",
                        "-Pincrement=$inc"
                    )
                    val result = Builder.build(project = project, args = args)
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    val nextVer = "0.1.1"
                    result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                    result.output shouldContain expectedTag(
                        project.name,
                        Semver(nextVer),
                        dryRun,
                        UpToDate.FALSE,
                        false
                    )
                }
            }

            it("should use defaultIncrement with -Prelease when release keyword is not present and -Pincrement is not set") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit").call()
                }
                // Act
                val args = if (!dryRun) arrayOf("tag", "-Prelease") else arrayOf("tag", "-PdryRun", "-Prelease")
                val result = Builder.build(project = project, args = args)
                // Assert
                result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                val nextVer = "0.2.0"
                result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                result.output shouldContain expectedTag(project.name, Semver(nextVer), dryRun, UpToDate.FALSE, false)
            }

            it("should do nothing if -Prelease not specified and release keyword not present") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit").call()
                }
                // Act
                // setting '-Pincrement' shouldn't do anything by itself
                val args = if (!dryRun) arrayOf("tag", "-Pincrement=major") else arrayOf(
                    "tag",
                    "-PdryRun",
                    "-Pincrement=major"
                )
                val result = Builder.build(project = project, args = args)
                // Assert
                result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                result.output shouldContain expectedConfigure(Semver("0.0.0"), emptyList())
                result.output shouldContain expectedTag(project.name, null, dryRun, UpToDate.TRUE, false)
            }
        }

        describe("pre-release version with${if (!dryRun) "" else " and dryRun"}") {
            listOf("0.0.1", "0.1.0", "1.0.0-rc.1", "1.0.0").forEach { version ->
                // pre-release with default increment when neither '-Pincrement' property nor commit message '[keyword]' is present
                it("should create new pre-release from $version with -Prelease -PpreRelease options") {
                    val project = SemverKtTestProject()
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v$version").call() // set initial version
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit").call()
                    }
                    // Act
                    val args = if (!dryRun) arrayOf("tag", "-Prelease", "-PpreRelease") else arrayOf(
                        "tag",
                        "-PdryRun",
                        "-Prelease",
                        "-PpreRelease"
                    )
                    val result = Builder.build(project = project, args = args)
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    // since no increment is set, using default one
                    val nextVer = when (version) { // new pre-release with DEFAULT increment
                        "0.0.1" -> "0.1.0-rc.1"
                        "0.1.0" -> "0.2.0-rc.1"
                        "1.0.0-rc.1", "1.0.0" -> "1.1.0-rc.1"
                        else -> "42" // shouldn't really get here
                    }
                    result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                    result.output shouldContain expectedTag(
                        project.name,
                        Semver(nextVer),
                        dryRun,
                        UpToDate.FALSE,
                        false
                    )
                }

                // pre-release from commit message
                listOf("major", "minor", "patch", "pre release").forEach { keyword ->
                    it("should create new pre-release from $version with -Prelease -PpreRelease options and [$keyword] keyword") {
                        val project = SemverKtTestProject()
                        // Arrange
                        Git.open(project.projectDir.toFile()).use {
                            it.tag().setName("v$version").call() // set initial version
                            project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                            it.add().addFilepattern("text.txt").call()
                            it.commit().setMessage("New commit\n\n[$keyword]").call()
                        }
                        // Act
                        val args = if (!dryRun) arrayOf("tag", "-Prelease", "-PpreRelease") else arrayOf(
                            "tag",
                            "-PdryRun",
                            "-Prelease",
                            "-PpreRelease"
                        )
                        val result = Builder.build(project = project, args = args)
                        // Assert
                        result.task(":tag")?.outcome shouldBe expectedOutcome(
                            dryRun,
                            UpToDate(keyword == "pre release")
                        )
                        val nextVer = when (keyword) {
                            "major" -> when (version) {
                                "0.0.1", "0.1.0" -> "1.0.0-rc.1"
                                "1.0.0-rc.1", "1.0.0" -> "2.0.0-rc.1"
                                else -> "42" // shouldn't really get here
                            }
                            "minor" -> when (version) {
                                "0.0.1" -> "0.1.0-rc.1"
                                "0.1.0" -> "0.2.0-rc.1"
                                "1.0.0-rc.1", "1.0.0" -> "1.1.0-rc.1"
                                else -> "42" // shouldn't really get here
                            }
                            "patch" -> when (version) {
                                "0.0.1" -> "0.0.2-rc.1"
                                "0.1.0" -> "0.1.1-rc.1"
                                "1.0.0-rc.1", "1.0.0" -> "1.0.1-rc.1"
                                else -> "42" // shouldn't really get here
                            }
                            "pre release" -> version // not a valid increment value
                            else -> "42" // shouldn't really get here
                        }

                        val v = if (keyword == "pre release") null else Semver(nextVer)
                        result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                        result.output shouldContain expectedTag(
                            project.name,
                            v,
                            dryRun,
                            UpToDate(keyword == "pre release"),
                            false
                        )
                    }
                }

                // pre-release from gradle properties
                listOf("major", "minor", "patch", "pre_release").forEach { inc ->
                    it("should create new pre-release from $version with -Prelease -PpreRelease -Pincrement=$inc options") {
                        val project = SemverKtTestProject()
                        // Arrange
                        Git.open(project.projectDir.toFile()).use {
                            it.tag().setName("v$version").call() // set initial version
                            project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                            it.add().addFilepattern("text.txt").call()
                            it.commit().setMessage("New commit").call()
                        }
                        // Act
                        val args = if (!dryRun) arrayOf(
                            "tag",
                            "-Prelease",
                            "-PpreRelease",
                            "-Pincrement=$inc"
                        ) else arrayOf("tag", "-PdryRun", "-Prelease", "-PpreRelease", "-Pincrement=$inc")
                        val result = Builder.build(project = project, args = args)
                        // Assert
                        result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate(inc == "pre_release"))
                        val nextVer = when (inc) {
                            "major" -> when (version) {
                                "0.0.1", "0.1.0" -> "1.0.0-rc.1"
                                "1.0.0-rc.1", "1.0.0" -> "2.0.0-rc.1"
                                else -> "42" // shouldn't really get here
                            }
                            "minor" -> when (version) {
                                "0.0.1" -> "0.1.0-rc.1"
                                "0.1.0" -> "0.2.0-rc.1"
                                "1.0.0-rc.1", "1.0.0" -> "1.1.0-rc.1"
                                else -> "42" // shouldn't really get here
                            }
                            "patch" -> when (version) {
                                "0.0.1" -> "0.0.2-rc.1"
                                "0.1.0" -> "0.1.1-rc.1"
                                "1.0.0-rc.1", "1.0.0" -> "1.0.1-rc.1"
                                else -> "42" // shouldn't really get here
                            }
                            "pre_release" -> version // not a valid increment value
                            else -> "42" // shouldn't really get here
                        }
                        val v = if (inc == "pre_release") null else Semver(nextVer)
                        result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                        result.output shouldContain expectedTag(
                            project.name,
                            v,
                            dryRun,
                            UpToDate(inc == "pre_release"),
                            false
                        )
                    }
                }
            }

            context("promote to release with -PpromoteRelease option") {
                listOf(
                    "0.1.0",
                    "1.0.0-rc.1",
                    "0.2.0-rc.123",
                    "0.2.0-rc.123+build.456",
                    "0.0.1-rc.1"
                ).forEach { version ->
                    it("should promote pre-release $version to release with -Prelease option") {
                        val project = SemverKtTestProject()
                        // Arrange
                        Git.open(project.projectDir.toFile()).use {
                            it.tag().setName("v$version").call() // set initial version
                            project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                            it.add().addFilepattern("text.txt").call()
                            it.commit().setMessage("New commit").call()
                        }
                        // Act
                        val args = if (!dryRun) arrayOf("tag", "-Prelease", "-PpromoteRelease") else arrayOf(
                            "tag",
                            "-PdryRun",
                            "-Prelease",
                            "-PpromoteRelease"
                        )
                        val result = Builder.build(project = project, args = args)
                        // Assert
                        result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate(version == "0.1.0"))
                        val nextVer = when (version) {
                            "0.1.0" -> "0.1.0" // since it's not a pre-release version, we can't really bump to next pre-release snapshot either
                            "1.0.0-rc.1" -> "1.0.0"
                            "0.2.0-rc.123", "0.2.0-rc.123+build.456" -> "0.2.0"
                            "0.0.1-rc.1" -> "0.0.1"
                            else -> "42" // shouldn't really get here
                        }
                        val v = if (version == "0.1.0") null else Semver(nextVer)
                        result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                        result.output shouldContain expectedTag(
                            project.name,
                            v,
                            dryRun,
                            UpToDate(version == "0.1.0"),
                            false
                        )
                    }
                }

                it("shouldn't do anything if -Prelease is NOT set and snapshots are disabled") {
                    val project = SemverKtTestProject()
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0-rc.1").call() // set initial version
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit").call()
                    }
                    // Act
                    val args = if (!dryRun) arrayOf("tag", "-PpromoteRelease") else arrayOf(
                        "tag",
                        "-PdryRun",
                        "-PpromoteRelease"
                    )
                    val result = Builder.build(project = project, args = args)
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                    result.output shouldContain expectedConfigure(Semver("0.0.0"), emptyList())
                    result.output shouldContain expectedTag(project.name, null, dryRun, UpToDate.TRUE, false)
                }
            }
        }

        describe("custom configuration${testName(fromCommit, dryRun)}") {
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
                    "rojam" to major,
                    "ronim" to minor,
                    "hctap" to patch,
                    "esaeler erp" to preRelease
                ).forEach { (keyword, r) ->
                    it("should set initial version via commit message with [$keyword] keyword") {
                        val project = SemverKtTestProject(configure = config)
                        // Arrange
                        Git.open(project.projectDir.toFile()).use {
                            project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                            it.add().addFilepattern("text.txt").call()
                            it.commit().setMessage("New commit\n\n[$keyword]").call()
                        }
                        // Act
                        val result = fromCommit.tag(project)(r)(dryRun)
                        // Assert
                        result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                        // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                        val v = if (keyword == "esaeler erp") "0.1.0-rc.1" else "0.1.0"
                        result.output shouldContain expectedConfigure(Semver(v), emptyList())
                        result.output shouldContain expectedTag(project.name, Semver(v), dryRun, UpToDate.FALSE, false)
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
                        val result = fromCommit.tag(project)(r)(dryRun)
                        // Assert
                        result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                        val nextVer = when (keyword) {
                            "rojam" -> "1.0.0"
                            "ronim" -> "0.2.0"
                            "hctap" -> "0.1.1"
                            "esaeler erp" -> "0.2.0-rc.1"
                            else -> "42" // shouldn't really get here
                        }
                        result.output shouldContain expectedConfigure(Semver(nextVer), emptyList())
                        result.output shouldContain expectedTag(
                            project.name,
                            Semver(nextVer),
                            dryRun,
                            UpToDate.FALSE,
                            false
                        )
                    }
                }
            }
        }

        describe("nextVersion < latestVersion${testName(fromCommit, dryRun)}") {
            it("should NOT set version without keyword in commit message") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call()
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit without release message").call()
                }
                // Act
                val result = fromCommit.tag(project)(minor)(dryRun)
                // Assert
                result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain expectedConfigure(Semver("0.0.0"), emptyList())
                result.output shouldContain expectedTag(project.name, null, dryRun, UpToDate.TRUE, false)
                result.output shouldNotContain "Calculated next version"
            }
        }
    }

    describe("current git HEAD with a version") {
        ReleaseFromCommit.forEach { rfc -> DryRun.forEach { dryRun ->
            it("should set version to current version with increment${testName(rfc, dryRun)}") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commitMinor(rfc)
                    it.tag().setName("v0.1.0").call() // add a tag manually on latest commit
                }
                // Act
                val result = rfc.tag(project)(minor)(dryRun)
                // Assert
                result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain expectedConfigure(Semver("0.1.0"), emptyList())
                result.output shouldContain "> Task :tag UP-TO-DATE\nCurrent version: 0.1.0"
                result.output shouldNotContain "Calculated next version"
            }
        } }
    }

    describe("re-run 'tag' after releasing a version") {
        ReleaseFromCommit.forEach { rfc -> DryRun.forEach { dryRun ->
            it("should return UP_TO_DATE with increment from commit${testName(rfc, dryRun)}") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commitMinor(rfc)
                }
                // release a version
                rfc.tag(project)(minor)(DryRun(false)).task(":tag")?.outcome shouldBe expectedOutcome(DryRun(false), UpToDate.FALSE)
                // Act
                // re-run :tag task after releasing a version
                val result = rfc.tag(project)(minor)(dryRun)
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                result.output shouldContain "> Configure project :\nProject test-project version: 0.1.0"
                result.output shouldContain "> Task :tag UP-TO-DATE"
                result.output shouldNotContain "Calculated next version"
            }
        } }
    }

    describe("!compatible gradle version") {
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
    describe("!incompatible gradle version") {
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

    describe("multi-module project") {
        val modules = listOf("core", "foo", "bar", "baz")
        ReleaseFromCommit.forEach { rfc -> DryRun.forEach { dryRun ->
            it("should set next version${testName(rfc, dryRun)}") {
                val project = SemverKtTestProject(multiModule = true, monorepo = false)
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    commit(project, it, if (!rfc) null else Increment.MINOR)(null)
                    it.commitMinor(rfc)
                }
                // Act
                val result = rfc.tag(project)(minor)(dryRun)
                // Assert
                val v = Semver("0.2.0")
                result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                result.output shouldContain expectedConfigure(v, emptyList())
                result.output shouldContain expectedTag(project.name, v, dryRun, UpToDate.FALSE, false)
                result.output shouldContain expectedTag("core", v, dryRun, UpToDate.TRUE, true)
                result.output shouldContain expectedTag("foo", v, dryRun, UpToDate.TRUE, true)
                result.output shouldContain expectedTag("bar", v, dryRun, UpToDate.TRUE, true)
                result.output shouldContain expectedTag("baz", v, dryRun, UpToDate.TRUE, true)
            }

            context("monorepo versioning${testName(rfc, dryRun)}") {
                it("should set next version") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        listOf("foo", "bar").forEach { module -> commit(project, it, if (!rfc) null else Increment.MINOR)(module) }
                        it.commitMinor(rfc)
                    }
                    // Act
                    val result = rfc.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(v, emptyList())
                    result.output shouldContain expectedTag(project.name, v, dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("foo", v, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("bar", v, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("core", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, true)
                }

                it("should set non-configured module version from root changes") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it, if (!rfc) null else Increment.MINOR)(null) // will trigger release of 'core'
                        it.commitMinor(rfc)
                    }
                    // Act
                    val result = rfc.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(v, emptyList())
                    result.output shouldContain expectedTag(project.name, v, dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("core", v, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("bar", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, true)
                }

                it("should set non-configured module version from root changes with another configured module changes") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it, if (!rfc) null else Increment.MINOR)(null) // will trigger release of 'core'
                        commit(project, it, if (!rfc) null else Increment.MINOR)("baz")
                        it.commitMinor(rfc)
                    }
                    // Act
                    val result = rfc.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(v, emptyList())
                    result.output shouldContain expectedTag(project.name, v, dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("core", v, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("baz", v, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("bar", null, dryRun, UpToDate.TRUE, true)
                }

                it("should always set version for root project") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it, if (!rfc) null else Increment.MINOR)("baz")
                        it.commitMinor(rfc)
                    }
                    // Act
                    val result = rfc.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(v, emptyList())
                    result.output shouldContain expectedTag(project.name, v, dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("bar", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("core", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("baz", v, dryRun, UpToDate.TRUE, true)
                }

                it("should NOT set non-configured module version") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it, if (!rfc) null else Increment.MINOR)("baz")
                        it.commitMinor(rfc)
                    }
                    // Act
                    val result = rfc.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(v, emptyList())
                    result.output shouldContain expectedTag(project.name, v, dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("baz", v, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("core", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, true)
                    result.output shouldContain expectedTag("bar", null, dryRun, UpToDate.TRUE, true)
                }

                listOf(null, "core", "foo").forEach { commitIn ->
                    it("should set ALL versions to next MAJOR when committing in $commitIn${testName(rfc, dryRun)}") {
                        // Arrange
                        val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                        Git.open(project.projectDir.toFile()).use {
                            it.tag().setName("v0.1.0").call() // set initial version
                            commit(project, it, if (!rfc) null else Increment.MAJOR)(commitIn)
                            it.commitMajor(rfc)
                        }
                        // Act
                        // -> tag next version with MAJOR release
                        val result = rfc.tag(project)(major)(dryRun)
                        // Assert
                        val initial = Semver("0.1.0")
                        result.output shouldContain expectedConfigure(initial.incrementMajor(), emptyList())
                        result.output shouldContain expectedTag(project.name, initial.incrementMajor(), dryRun, UpToDate.FALSE, false)
                        modules.forEach { m ->
                            result.output shouldContain expectedTag(m, initial.incrementMajor(), dryRun, UpToDate.TRUE, true)
                        }
                    }
                }
            }

            @Suppress("DestructuringWrongName")
            context("monorepo versioning with multi-tagging${testName(rfc, dryRun)}") {
                it("should set initial version") {
                    // arrange
                    val initial = Semver("0.1.0")
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true)
                    Git.open(project.projectDir.toFile()).use {
                        // -> clean (no tags) repo with changes in 'root' and all modules (':core', ':foo', ':bar', ':baz')
                        project.projectDir.resolve("text.txt").createFile().writeText(faker.lorem.words())
                        it.add().addFilepattern(".").call()
                        it.commit().setMessage("First commit").call()
                        modules.forEach { m -> commit(project, it, if (!rfc) null else Increment.MINOR)(m) }
                    }
                    // act
                    // -> tag next minor version
                    val result = rfc.tag(project)(minor)(dryRun)
                    // assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    // -> 'root' should have initial version
                    result.output shouldContain expectedConfigure(initial, modules.map { it to initial })
                    result.output shouldContain expectedTag(project.name, initial, dryRun, UpToDate.FALSE, false)
                    modules.forEach {
                        // -> all submodules should have initial version
                        // -> ':core' submodule is NOT configured with custom tag prefix and should have 'root' tag)
                        // -> ':foo', ':bar', ':baz' submodules are configured with custom tag prefix and should have their own tags)
                        result.output shouldContain expectedTag(it, initial, dryRun, if (it == "core") UpToDate.TRUE else UpToDate.FALSE, it == "core")
                    }
                }

                it("should ONLY set version for changed submodules") {
                    // arrange
                    // -> repo with initial version for each tag-prefix (v0.1.0, foo-v0.1.0, bar-v0.1.0, baz-v0.1.0)
                    val initial = Semver("0.1.0")
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true)
                    val modulesVersions = modules.map { it to initial }
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in 'root' project path
                        commit(project, it, if (!rfc) null else Increment.MINOR)(null)
                        // -> commit file in ':foo' submodule path
                        commit(project, it, if (!rfc) null else Increment.MINOR)("foo")
                    }
                    // act
                    // -> tag next minor version
                    val result = rfc.tag(project)(minor)(dryRun)
                    // assert
                    val ml = modulesVersions.map { (m, v) ->
                        if (m == "core" || m == "foo") m to v.incrementMinor() else m to v
                    }
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(initial.incrementMinor(), ml)
                    // -> 'root' project and ':core' submodule should have next version tag (v0.2.0)
                    //    ('root' has changes and ':core' is not configured with custom tag prefix)
                    result.output shouldContain expectedTag(project.name, initial.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("core", initial.incrementMinor(), dryRun, UpToDate.TRUE, true)
                    // -> ':foo' submodule should have next version tag (foo-v0.2.0)
                    //    (configured with custom tag prefix and has changes)
                    result.output shouldContain expectedTag("foo", initial.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    // -> ':bar' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expectedTag("bar", null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, false)
                }

                it("should set version for ROOT when non-configured submodule has changes") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v0.2.0, foo-v0.2.0, bar-v0.1.0, baz-v0.1.0)
                    val (second, initial) = Semver("0.2.0") to Semver("0.1.0")
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true)
                    val modulesVersions = listOf("core" to second, "foo" to second, "bar" to initial, "baz" to initial)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':core' submodule path
                        commit(project, it, if (!rfc) null else Increment.MINOR)("core")
                        // -> commit file in ':bar' submodule path
                        commit(project, it, if (!rfc) null else Increment.MINOR)("bar")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = rfc.tag(project)(minor)(dryRun)
                    // assert
                    val ml = modulesVersions.map { (m, v) ->
                        if (m == "core" || m == "bar") m to v.incrementMinor() else m to v
                    }
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(second.incrementMinor(), ml)
                    // -> 'root' project and ':core' submodule should have next version tag (v0.3.0)
                    //    (':core' is not configured with custom tag prefix and has changes)
                    result.output shouldContain expectedTag(project.name, second.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("core", second.incrementMinor(), dryRun, UpToDate.TRUE, true)
                    // -> ':bar' submodule should have next version tag (bar-v0.2.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expectedTag("bar", initial.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, false)
                }

                it("should NOT set version for ROOT w/o changes") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v0.3.0, foo-v0.2.0, bar-v0.2.0, baz-v0.1.0)
                    val (third, second, initial) = Triple(Semver("0.3.0"), Semver("0.2.0"), Semver("0.1.0"))
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true)
                    val modulesVersions = listOf("core" to third, "foo" to second, "bar" to second, "baz" to initial)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':bar' submodule path
                        commit(project, it, if (!rfc) null else Increment.MINOR)("bar")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = rfc.tag(project)(minor)(dryRun)
                    // assert
                    val ml = modulesVersions.map { (m, v) ->
                        if (m == "bar") m to v.incrementMinor() else m to v
                    }
                    // -> 'root' project and ':core' submodule should be unchanged
                    //    (we have custom tags for some submodules, and neither 'root' nor ':core' have changes)
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                    result.output shouldContain expectedConfigure(third, ml)
                    result.output shouldContain expectedTag(project.name, null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("core", null, dryRun, UpToDate.TRUE, false)
                    // -> ':bar' submodule should have next version tag (bar-v0.3.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expectedTag("bar", second.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, false)
                }

                it("should set initial version for new submodule before first stable release") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v0.3.0, foo-v0.2.0, bar-v0.2.0, baz-v0.1.0)
                    val (third, second, initial) = Triple(Semver("0.3.0"), Semver("0.2.0"), Semver("0.1.0"))
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true)
                    val modulesVersions = listOf("core" to third, "foo" to second, "bar" to second, "baz" to initial)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':bar' submodule path
                        commit(project, it, if (!rfc) null else Increment.MINOR)("bar")
                        // -> add new ':zoo' submodule
                        project.add(ModuleConfig("zoo", tag = object : GitTagConfig {
                            override val prefix: TagPrefix = TagPrefix("zoo-v")
                        }))
                        commit(project, it, if (!rfc) null else Increment.MINOR)("zoo")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = rfc.tag(project)(minor)(dryRun)
                    // assert
                    val ml = modulesVersions.plus("zoo" to initial).map { (m, v) ->
                        if (m == "core" || m == "bar") m to v.incrementMinor() else m to v
                    }
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(third.incrementMinor(), ml)
                    // -> 'root' project and ':core' submodule should have next version tag (v0.4.0)
                    //    (we have modified root settings.gradle.kts by adding a new submodule)
                    result.output shouldContain expectedTag(project.name, third.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("core", third.incrementMinor(), dryRun, UpToDate.TRUE, true)
                    // -> ':bar' submodule should have next version tag (bar-v0.3.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expectedTag("bar", second.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, false)
                    // -> ':zoo' submodule should have initial version tag (bar-v0.1.0)
                    //    (configured with custom tag prefix and "default initial version in pre-stable" -> '${rootVersion.major}.1.0')
                    result.output shouldContain expectedTag("zoo", initial, dryRun, UpToDate.FALSE, false)
                }

                listOf(null, "core", "foo").forEach { commitIn ->
                    it("should set ALL versions to next MAJOR when committing in $commitIn") {
                        // arrange
                        // -> repo with tags for each tag-prefix (v0.3.0, foo-v0.2.0, bar-v0.3.0, baz-v0.1.0)
                        val (third, second, initial) = Triple(Semver("0.3.0"), Semver("0.2.0"), Semver("0.1.0"))
                        val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true).also {
                            // -> add new ':zoo' submodule
                            it.add(ModuleConfig("zoo", tag = object : GitTagConfig {
                                override val prefix: TagPrefix = TagPrefix("zoo-v")
                            }))
                        }
                        // -> add initial version tag for ':zoo' (zoo-v0.1.0)
                        val modulesVersions = listOf("core" to third, "foo" to second, "bar" to second, "baz" to initial, "zoo" to initial)
                        Git.open(project.projectDir.toFile()).use {
                            setupInitialVersions(project, it)(modulesVersions)
                            // -> commit file in ':core' submodule path
                            commit(project, it, if (!rfc) null else Increment.MAJOR)(commitIn)
                        }
                        // act
                        // -> tag next version with MAJOR release
                        val result = rfc.tag(project)(major)(dryRun)
                        // assert
                        result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                        // -> 'root' and all submodules should have next MAJOR version (1.0.0)
                        //    (':core' submodule is NOT configured with custom tag prefix and should have 'root' tag prefix)
                        //    (':foo', ':bar', ':baz', ':zoo' submodules are configured with custom tag prefix and should have their own tags)
                        result.output shouldContain expectedConfigure(third.incrementMajor(), modulesVersions.map { it.first to it.second.incrementMajor() })
                        result.output shouldContain expectedTag(project.name, third.incrementMajor(), dryRun, UpToDate.FALSE, false)
                        modulesVersions.forEach { (m, v) ->
                            result.output shouldContain expectedTag(m, v.incrementMajor(), dryRun, if (m == "core") UpToDate.TRUE else UpToDate.FALSE, m == "core")
                        }
                    }
                }

                it("should set initial version for new submodule after first stable release") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v1.0.0, foo-v1.0.0, bar-v1.0.0, baz-v1.0.0)
                    val ver = Semver("1.3.0")
                    val modulesVersions = modules.map { it to ver }
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':core' submodule path
                        commit(project, it, if (!rfc) null else Increment.MINOR)("core")
                        // -> commit file in ':bar' submodule path
                        commit(project, it, if (!rfc) null else Increment.MINOR)("bar")
                        // -> add new ':abc' submodule
                        project.add(ModuleConfig("abc", tag = object : GitTagConfig {
                            override val prefix: TagPrefix = TagPrefix("abc-v")
                        }))
                        commit(project, it, if (!rfc) null else Increment.MINOR)("abc")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = rfc.tag(project)(minor)(dryRun)
                    // assert
                    val ml = modulesVersions.plus("abc" to Semver("1.0.0")).map { (m, v) ->
                        if (m == "core" || m == "bar") m to v.incrementMinor() else m to v
                    }
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain expectedConfigure(ver.incrementMinor(), ml)
                    // -> 'root' project and ':core' submodule should have next version tag (v1.1.0)
                    //    (':core' is not configured with custom tag prefix and has changes)
                    result.output shouldContain expectedTag(project.name, ver.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("core", ver.incrementMinor(), dryRun, UpToDate.TRUE, true)
                    // -> ':bar' submodule should have next version tag (bar-v0.3.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expectedTag("bar", ver.incrementMinor(), dryRun, UpToDate.FALSE, false)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expectedTag("foo", null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, false)
                    // -> ':abc' submodule should have initial version tag (bar-v1.0.0)
                    //    (configured with custom tag prefix and "default initial version" -> '${rootVersion.major}.0.0')
                    result.output shouldContain expectedTag("abc", Semver("1.0.0"), dryRun, UpToDate.FALSE, false)
                }

                it("should set next version") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true, printModuleVersion = true)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        it.tag().setName("bar-v0.2.0").call() // set initial version for 'bar' module
                        listOf("foo", "bar").forEach { module -> commit(project, it, if (!rfc) null else Increment.MINOR)(module) }
                    }
                    // Act
                    val result = rfc.tag(project)(minor)(dryRun)
                    // Assert
                    val expectedVersions = listOf("core" to Semver("0.1.0"), "foo" to Semver("0.1.0"), "bar" to Semver("0.3.0"), "baz" to Semver("0.0.0"))
                        .sortedBy { (m, _) -> m }
                    result.task(":tag")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                    result.output shouldContain expectedConfigure(Semver("0.1.0"), expectedVersions)
                    result.output shouldContain expectedTag(project.name, null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("core", null, dryRun, UpToDate.TRUE, false)
                    result.output shouldContain expectedTag("foo", Semver("0.1.0"), dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("bar", Semver("0.3.0"), dryRun, UpToDate.FALSE, false)
                    result.output shouldContain expectedTag("baz", null, dryRun, UpToDate.TRUE, false)
                }
            }
        } }
    }

    describe("setting version manually") {
        DryRun.forEach { dryRun ->
            listOf("major", "minor", "patch", "pre release").forEach { keyword ->
                it("should set version via -Pversion even when commit message has [$keyword] keyword${testName(fromCommit, dryRun)}") {
                    val project = SemverKtTestProject()
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit\n\n[$keyword]").call()
                    }
                    // Act
                    val args = arrayOf("tag", "-Pversion=42.0.0")
                    val result = Builder.build(
                        project = project,
                        args = if (!dryRun) args else args.plus("-PdryRun")
                    )
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain "Calculated next version: 42.0.0"
                }
            }
            listOf("major", "minor", "patch", "pre_release").forEach { keyword ->
                it("should set version via -Pversion even when -Prelease -Pincrement=$keyword is used${testName(fromProperty, dryRun)}") {
                    val project = SemverKtTestProject()
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit").call()
                    }
                    // Act
                    val args = arrayOf("tag", "-Prelease", "-Pincrement=$keyword", "-Pversion=42.0.0")
                    val result = Builder.build(
                        project = project,
                        args = if (!dryRun) args else args.plus("-PdryRun")
                    )
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                    result.output shouldContain "Calculated next version: 42.0.0"
                }
            }
        }
    }

    DryRun.forEach { dryRun ->
        describe("snapshot versions${testName(fromProperty, dryRun)}") {
            listOf("0.1.0", "0.1.0-rc.123", "0.1.0-rc.123+build.456").forEach { version ->
                listOf("major" to major, "minor" to minor, "patch" to patch, "pre_release" to preRelease).forEach { (inc, r) ->
                    it("should set next snapshot version for version $version with -Pincrement=$inc") {
                        val project = SemverKtTestProject(useSnapshots = true)
                        // Arrange
                        Git.open(project.projectDir.toFile()).use {
                            it.tag().setName("v$version").call() // set initial version
                            project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                            it.add().addFilepattern("text.txt").call()
                            it.commit().setMessage("New commit").call()
                        }
                        // Act
                        val args = arrayOf("tag", "-Pincrement=$inc")
                        val result = Builder.build(project = project, args = if (!dryRun) args else args.plus("-PdryRun"))
                        // Assert
                        result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                        val nextVer = when (inc) {
                            "major" -> "1.0.0-SNAPSHOT"
                            "minor" -> "0.2.0-SNAPSHOT"
                            "patch" -> "0.1.1-SNAPSHOT"
                            "pre_release" -> when (version) {
                                "0.1.0" -> "0.1.0" // since it's not a pre-release version, we can't really bump to next pre-release snapshot either
                                "0.1.0-rc.123", "0.1.0-rc.123+build.456" -> "0.1.0-rc.124-SNAPSHOT"
                                else -> "42"
                            }
                            else -> "42" // shouldn't really get here
                        }
                        result.output shouldContain """
                        > Configure project :
                        Project test-project version: $nextVer

                        > Task :tag UP-TO-DATE
                        ${if (version == "0.1.0" && inc == "pre_release") "Not doing anything" else "Snapshot version, not doing anything"}
                        """.trimIndent()
                    }
                }

                it("should set next snapshot for version $version") {
                    val project = SemverKtTestProject(useSnapshots = true)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v$version").call() // set initial version
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit").call()
                    }
                    // Act
                    val args = arrayOf("tag")
                    val result = Builder.build(project = project, args = if (!dryRun) args else args.plus("-PdryRun"))
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                    result.output shouldContain """
                    > Configure project :
                    Project test-project version: 0.2.0-SNAPSHOT

                    > Task :tag UP-TO-DATE
                    Snapshot version, not doing anything
                    """.trimIndent()
                }
            }

            context("SNAPSHOT version in pre-release with -PpromoteRelease") {
                it("should set next snapshot version") {
                    val project = SemverKtTestProject(useSnapshots = true)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v1.0.0-rc.1").call() // set initial version
                        project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                        it.add().addFilepattern("text.txt").call()
                        it.commit().setMessage("New commit").call()
                    }
                    // Act
                    val args = arrayOf("tag", "-PpromoteRelease")
                    val result = Builder.build(project = project, args = if (!dryRun) args else args.plus("-PdryRun"))
                    // Assert
                    result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                    // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                    result.output shouldContain """
                    > Configure project :
                    Project test-project version: 1.0.0-SNAPSHOT

                    > Task :tag UP-TO-DATE
                    Snapshot version, not doing anything
                    """.trimIndent()
                }
            }

            it("should never create a tag for a snapshot version") {
                val project = SemverKtTestProject(useSnapshots = true)
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit").call()
                }
                // Act
                val args = arrayOf("tag")
                val result = Builder.build(project = project, args = if (!dryRun) args else args.plus("-PdryRun"))
                // Assert
                result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.TRUE)
                result.output shouldContain """
                > Configure project :
                Project test-project version: 0.2.0-SNAPSHOT

                > Task :tag UP-TO-DATE
                Snapshot version, not doing anything
                """.trimIndent()
                with(Git.open(project.projectDir.toFile()).tagList().call()) {
                    size shouldBe 1
                    first().name shouldBe "refs/tags/v0.1.0"
                    last().name shouldBe "refs/tags/v0.1.0"
                }
            }
        }
    }

    describe("repository with uncommitted changes") {
        val p: (cl: CleanRule) -> SemverKtTestProject = { cl ->
            SemverKtTestProject(configure = { _ ->
                val repo: GitRepoConfig = object : GitRepoConfig {
                    override val cleanRule: CleanRule = cl
                }
                PojoConfiguration(gitRepoConfig = repo)
            })
        }
        ReleaseFromCommit.forEach { rfc -> DryRun.forEach { dryRun ->
            it("should return error when trying to release new version${testName(rfc, dryRun)} with any difference between working tree and HEAD") {
                val project = p(CleanRule.ALL)
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").writeText("Hello world")
                    it.add().addFilepattern("text.txt").call()
                    it.commitMinor(rfc)
                    project.projectDir.resolve("untracked.txt").createFile().writeText("Hello, World!")
                }
                // Act
                val args = if (dryRun.value) mutableListOf("tag", "-PdryRun") else mutableListOf("tag")
                // add args for non-commit based release
                if (!rfc) { args.add("-Prelease"); args.add("-Pincrement=minor") }
                val result = if (dryRun.value) Builder.build(project = project, args = args.toTypedArray())
                else Builder.buildAndFail(project = project, args = args.toTypedArray())
                // Assert
                result.task(":tag")?.outcome shouldBe if (dryRun.value) TaskOutcome.UP_TO_DATE else TaskOutcome.FAILED
                if (!dryRun) result.output shouldContain "Release with non-clean repository is not allowed"
            }

            it("should return error when trying to release new version${testName(rfc, dryRun)} with changes to tracked files") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.tag().setName("v0.1.0").call() // set initial version
                    it.commitMinor(rfc)
                    project.projectDir.resolve("text.txt").writeText("Hello, World!")
                }
                // Act
                val args = if (dryRun.value) mutableListOf("tag", "-PdryRun") else mutableListOf("tag")
                // add args for non-commit based release
                if (!rfc) { args.add("-Prelease"); args.add("-Pincrement=minor") }
                val result = if (dryRun.value) Builder.build(project = project, args = args.toTypedArray())
                else Builder.buildAndFail(project = project, args = args.toTypedArray())
                // Assert
                result.task(":tag")?.outcome shouldBe if (dryRun.value) TaskOutcome.UP_TO_DATE else TaskOutcome.FAILED
                if (!dryRun) result.output shouldContain "Release with uncommitted changes is not allowed"
            }

            it("should allow releasing a new version${testName(rfc, dryRun)} with disabled clean rule") {
                val project = p(CleanRule.NONE)
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.tag().setName("v0.1.0").call() // set initial version
                    it.commitMinor(rfc)
                    project.projectDir.resolve("text.txt").writeText("Hello, World!")
                    project.projectDir.resolve("untracked.txt").createFile().writeText("Hello, World!")
                }
                // Act
                val result = rfc.tag(project)(minor)(dryRun)
                // Assert
                result.task(":tag")?.outcome shouldBe expectedOutcome(dryRun, UpToDate.FALSE)
                result.output shouldNotContain "Release with uncommitted changes is not allowed"
            }
        } }
    }
})

val faker = faker {  }

// appends string to the test describe/context name
val testName: (ReleaseFromCommit, DryRun) -> String = { release, dryRun ->
    val s = StringBuilder()
    if (release.value) s.append(" via commit") else s.append(" via gradle property")
    if (dryRun.value) s.append(" and dryRun")
    s.toString()
}

val Git.commitMinor: (release: ReleaseFromCommit) -> Unit get() = {
    if (it.value) commit().setMessage("New commit\n\n[minor]").call()
    else commit().setMessage("New commit").call()
}

val Git.commitMajor: (release: ReleaseFromCommit) -> Unit get() = {
    if (it.value) commit().setMessage("New commit\n\n[major]").call()
    else commit().setMessage("New commit").call()
}

/**
 * Expected :tag task output for a project/module with [name]
 */
val expectedTag: (name: String, ver: Semver?, dryRun: DryRun, upToDate: UpToDate, tagExists: Boolean) -> String = { name, version, dryRun, upToDate, tagExists ->
    val noop = "Not doing anything"
    val sb = StringBuilder()
    sb.append("> Task :${if (name == "test-project") "" else "$name:"}tag")
    // when (dryRun) -> UP-TO-DATE will always be true
    upToDate(sb)(dryRun.value || upToDate.value)
    version?.let {
        sb.appendLine("Calculated next version: $it")
        if (!dryRun && tagExists) sb.appendLine("Tag v$it already exists in project")
    } ?: sb.appendLine(noop)
    sb.toString()
}

/**
 * Expected configure project output for a project [ver] with an optional [tag] and a list of [moduleVersions]
 */
val expectedConfigure: (ver: Semver, modulesVersions: List<Pair<String, Semver>>) -> String = { version, modules ->
    val sb = StringBuilder()
    sb.appendLine("> Configure project :")
    sb.appendLine("Project test-project version: $version")
    modules.sortedBy { (m, _) -> m }.forEach { (m, v) ->
        sb.appendLine()
        sb.appendLine("> Configure project :$m")
        sb.appendLine("Module $m version: $v")
    }
    sb.toString()
}

val expectedOutcome: (dryRun: DryRun, upToDate: UpToDate) -> TaskOutcome = { dryRun, upToDate ->
    if (!dryRun && !upToDate) TaskOutcome.SUCCESS else TaskOutcome.UP_TO_DATE
}

val setupInitialVersions: (proj: AbstractProject, git: Git) -> (modules: List<Pair<String, Semver>>) -> Unit = { proj, git ->
    { modules ->
        proj.projectDir.resolve("text.txt").createFile().writeText(faker.lorem.words())
        git.add().addFilepattern(".").call()
        git.commit().setMessage("First commit").call()

        modules.shuffled().forEach { (m, v) ->
            val tagName = if (m == "core") "v$v" else "$m-v$v"
            // commit in 'root' together with 'core'
            if (m == "core") commit(proj, git, null)(null)
            commit(proj, git, null)(m)
            git.tag().setName(tagName).call()
        }
    }
}

/**
 * Commit to a given [module] (or in the root of the project if module is `null`) in a [project].
 */
val commit: (project: AbstractProject, git: Git, inc: Increment?) -> (module: String?) -> Unit = { proj, git, inc ->
    { m ->
        if (m != null) {
            proj.projectDir.resolve(m)
                .resolve(if (m == "core") "src" else "${m}src").createDirectories()
                .resolve("${faker.random.nextUUID()}.txt")
                .createFile()
                .writeText(faker.lorem.words())
            git.add().addFilepattern(".").call()
            val msg = inc?.let {
                val increment = if (it == Increment.PRE_RELEASE) "pre release" else it.toString()
                "Commit in $m\n\n[$increment]"
            } ?: "Commit in $m$"
            git.commit().setMessage(msg).call()
        } else {
            proj.projectDir.resolve("${faker.random.nextUUID()}.txt")
                .createFile()
                .writeText(faker.lorem.words())
            git.add().addFilepattern(".").call()
            val msg = inc?.let {
                val increment = if (it == Increment.PRE_RELEASE) "pre release" else it.toString()
                "Commit in root\n\n[$increment]"
            } ?: "Commit in root"
            git.commit().setMessage(msg).call()
        }
    }
}

val upToDate: (StringBuilder) -> (Boolean) -> StringBuilder = { sb ->
    { upToDate -> if (upToDate) sb.appendLine(" UP-TO-DATE") else sb.appendLine() }
}
