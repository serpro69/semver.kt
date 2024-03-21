package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.kfaker.faker
import io.github.serpro69.semverkt.gradle.plugin.fixture.AbstractProject
import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.github.serpro69.semverkt.gradle.plugin.util.DryRun
import io.github.serpro69.semverkt.gradle.plugin.util.Release
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
        listOf("major", "minor", "patch", "pre_release").forEach { inc ->
            it("should set initial version via -Prelease -Pincrement=$inc") {
                val project = SemverKtTestProject()
                // Act
                val result =
                    Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Prelease", "-Pincrement=$inc"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                if (inc == "pre_release") result.output shouldContain "Calculated next version: 0.1.0-rc.1"
                else result.output shouldContain "Calculated next version: 0.1.0"
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
                val result =
                    Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Prelease", "-Pincrement=$inc"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                val nextVer = when (inc) {
                    "major" -> "1.0.0"
                    "minor" -> "0.2.0"
                    "patch" -> "0.1.1"
                    "pre_release" -> "0.2.0-rc.1"
                    else -> "42" // shouldn't really get here
                }
                result.output shouldContain "Calculated next version: $nextVer"
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
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Pincrement=$inc"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain """
                    > Configure project :
                    Project test-project version: 0.1.1

                    > Task :tag
                    Calculated next version: 0.1.1
                """.trimIndent()
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
            val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Prelease"))
            // Assert
            result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain """
                > Configure project :
                Project test-project version: 0.2.0

                > Task :tag
                Calculated next version: 0.2.0
            """.trimIndent()
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
            val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Pincrement=major"))
            // Assert
            result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain """
                > Configure project :
                Project test-project version: 0.0.0

                > Task :tag
                Not doing anything
            """.trimIndent()
        }
    }

    describe("pre-release version") {
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
                val result = Builder.build(
                    project = project,
                    args = arrayOf("tag", "-PdryRun", "-Prelease", "-PpreRelease")
                )
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // since no increment is set, using default one
                val nextVer = when (version) { // new pre-release with DEFAULT increment
                    "0.0.1" -> "0.1.0-rc.1"
                    "0.1.0" -> "0.2.0-rc.1"
                    "1.0.0-rc.1", "1.0.0" -> "1.1.0-rc.1"
                    else -> "42" // shouldn't really get here
                }
                result.output shouldContain """
                        > Configure project :
                        Project test-project version: $nextVer

                        > Task :tag
                        Calculated next version: $nextVer
                    """.trimIndent()
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
                    val result = Builder.build(
                        project = project,
                        args = arrayOf("tag", "-PdryRun", "-Prelease", "-PpreRelease")
                    )
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
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
                    result.output shouldContain """
                        > Configure project :
                        Project test-project version: $nextVer

                        > Task :tag
                        ${if (keyword == "pre release") "Not doing anything" else "Calculated next version: $nextVer"}
                    """.trimIndent()
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
                    val result = Builder.build(
                        project = project,
                        args = arrayOf("tag", "-PdryRun", "-Prelease", "-PpreRelease", "-Pincrement=$inc")
                    )
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
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
                    result.output shouldContain """
                        > Configure project :
                        Project test-project version: $nextVer

                        > Task :tag
                        ${if (inc == "pre_release") "Not doing anything" else "Calculated next version: $nextVer"}
                    """.trimIndent()
                }
            }
        }

        context("promote to release with -PpromoteRelease option") {
            listOf("0.1.0", "1.0.0-rc.1", "0.2.0-rc.123", "0.2.0-rc.123+build.456", "0.0.1-rc.1").forEach { version ->
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
                    val result = Builder.build(
                        project = project,
                        args = arrayOf("tag", "-PdryRun", "-Prelease", "-PpromoteRelease")
                    )
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    val nextVer = when (version) {
                        "0.1.0" -> "0.1.0" // since it's not a pre-release version, we can't really bump to next pre-release snapshot either
                        "1.0.0-rc.1" -> "1.0.0"
                        "0.2.0-rc.123", "0.2.0-rc.123+build.456" -> "0.2.0"
                        "0.0.1-rc.1" -> "0.0.1"
                        else -> "42" // shouldn't really get here
                    }
                    result.output shouldContain """
                        > Configure project :
                        Project test-project version: $nextVer

                        > Task :tag
                        ${if (version == "0.1.0") "Not doing anything" else "Calculated next version: $nextVer"}
                    """.trimIndent()
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
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-PpromoteRelease"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain """
                    > Configure project :
                    Project test-project version: 0.0.0

                    > Task :tag
                    Not doing anything
                """.trimIndent()
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
            // Arrange
            Git.open(project.projectDir.toFile()).use {
                it.tag().setName("v0.1.0").call()
                project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                it.add().addFilepattern("text.txt").call()
                it.commit().setMessage("New commit without release message").call()
            }
            // Act
            val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
            // Assert
            result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
            // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
            result.output shouldContain "> Configure project :\nProject test-project version: 0.0.0"
            result.output shouldContain "> Task :tag\nNot doing anything"
            result.output shouldNotContain "Calculated next version"
        }
    }

    describe("current git HEAD with a version") {
        Release.forEach { release -> DryRun.forEach { dryRun ->
            it("should set version to current version with increment${testName(release, dryRun)}") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commitMinor(release)
                    it.tag().setName("v0.1.0").call() // add a tag manually on latest commit
                }
                // Act
                val result = release.tag(project)(minor)(dryRun)
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain "> Configure project :\nProject test-project version: 0.1.0"
                result.output shouldContain "> Task :tag\nCurrent version: 0.1.0"
                result.output shouldNotContain "Calculated next version"
            }
        } }
    }

    describe("re-run 'tag' after releasing a version") {
        Release.forEach { release -> DryRun.forEach { dryRun ->
            it("should return UP_TO_DATE with increment from commit${testName(release, dryRun)}") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commitMinor(release)
                }
                // release a version
                release.tag(project)(minor)(DryRun(false))
                // Act
                // re-run :tag task after releasing a version
                val result = release.tag(project)(minor)(dryRun)
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                result.output shouldContain "> Configure project :\nProject test-project version: 0.1.0"
                result.output shouldContain "> Task :tag UP-TO-DATE"
                result.output shouldNotContain "Calculated next version"
            }
        } }
    }

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
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
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
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
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
        Release.forEach { release -> DryRun.forEach { dryRun ->
            it("should set next version${testName(release, dryRun)}") {
                val project = SemverKtTestProject(multiModule = true, monorepo = false)
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    commit(project, it)(null)
                    it.commitMinor(release)
                }
                // Act
                val result = release.tag(project)(minor)(dryRun)
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                val v = Semver("0.2.0")
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain expected(project.name, v, dryRun)
                result.output shouldContain expected("core", v, dryRun)
                result.output shouldContain expected("foo", v, dryRun)
                result.output shouldContain expected("bar", v, dryRun)
                result.output shouldContain expected("baz", v, dryRun)
                if (!dryRun) {
                    result.output shouldContain tagExists("core", v, dryRun)
                    result.output shouldContain tagExists("foo", v, dryRun)
                    result.output shouldContain tagExists("bar", v, dryRun)
                    result.output shouldContain tagExists("baz", v, dryRun)
                }
            }

            context("monorepo versioning${testName(release, dryRun)}") {
                it("should set next version") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        listOf("foo", "bar").forEach { module -> commit(project, it)(module) }
                        it.commitMinor(release)
                    }
                    // Act
                    val result = release.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    result.output shouldContain expected(project.name, v, dryRun)
                    result.output shouldContain expected("foo", v, dryRun)
                    result.output shouldContain expected("bar", v, dryRun)
                    result.output shouldContain expected("core", null, dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                    if (!dryRun) {
                        result.output shouldContain tagExists("foo", v, dryRun)
                        result.output shouldContain tagExists("bar", v, dryRun)
                        result.output shouldNotContain tagExists("core", v, dryRun)
                        result.output shouldNotContain tagExists("baz", v, dryRun)
                    }
                }

                it("should set non-configured module version from root changes") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it)(null) // will trigger release of 'core'
                        it.commitMinor(release)
                    }
                    // Act
                    val result = release.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    result.output shouldContain expected(project.name, v, dryRun)
                    result.output shouldContain expected("core", v, dryRun)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("bar", null, dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                    if (!dryRun) {
                        if (!dryRun) result.output shouldContain tagExists("core", v, dryRun)
                        result.output shouldNotContain tagExists("foo", v, dryRun)
                        result.output shouldNotContain tagExists("bar", v, dryRun)
                        result.output shouldNotContain tagExists("baz", v, dryRun)
                    }
                }

                it("should set non-configured module version from root changes with another configured module changes") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it)(null) // will trigger release of 'core'
                        commit(project, it)("baz")
                        it.commitMinor(release)
                    }
                    // Act
                    val result = release.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    result.output shouldContain expected(project.name, v, dryRun)
                    result.output shouldContain expected("core", v, dryRun)
                    result.output shouldContain expected("baz", v, dryRun)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("bar", null, dryRun)
                    if (!dryRun) {
                        if (!dryRun) result.output shouldContain tagExists("core", v, dryRun)
                        if (!dryRun) result.output shouldContain tagExists("baz", v, dryRun)
                        result.output shouldNotContain tagExists("foo", v, dryRun)
                        result.output shouldNotContain tagExists("bar", v, dryRun)
                    }
                }

                it("should always set version for root project") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it)("baz")
                        it.commitMinor(release)
                    }
                    // Act
                    val result = release.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    result.output shouldContain expected(project.name, v, dryRun)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("bar", null, dryRun)
                    result.output shouldContain expected("core", null, dryRun)
                    result.output shouldContain expected("baz", v, dryRun)
                    if (!dryRun) {
                        result.output shouldNotContain tagExists("core", v, dryRun)
                        result.output shouldNotContain tagExists("foo", v, dryRun)
                        result.output shouldNotContain tagExists("bar", v, dryRun)
                        if (!dryRun) result.output shouldContain tagExists("baz", v, dryRun)
                    }
                }

                it("should NOT set non-configured module version") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        commit(project, it)("baz")
                        it.commitMinor(release)
                    }
                    // Act
                    val result = release.tag(project)(minor)(dryRun)
                    // Assert
                    val v = Semver("0.2.0")
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    result.output shouldContain expected(project.name, v, dryRun)
                    result.output shouldContain expected("baz", v, dryRun)
                    result.output shouldContain expected("core", null, dryRun)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("bar", null, dryRun)
                    if (!dryRun) {
                        if (!dryRun) result.output shouldContain tagExists("baz", v, dryRun)
                        result.output shouldNotContain tagExists("core", v, dryRun)
                        result.output shouldNotContain tagExists("foo", v, dryRun)
                        result.output shouldNotContain tagExists("bar", v, dryRun)
                    }
                }

                listOf(null, "core", "foo").forEach { commitIn ->
                    it("should set ALL versions to next MAJOR when committing in $commitIn${testName(release, dryRun)}") {
                        // Arrange
                        val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = false)
                        Git.open(project.projectDir.toFile()).use {
                            it.tag().setName("v0.1.0").call() // set initial version
                            commit(project, it)(commitIn)
                            it.commitMajor(release)
                        }
                        // Act
                        // -> tag next version with MAJOR release
                        val result = release.tag(project)(major)(dryRun)
                        // Assert
                        val initial = Semver("0.1.0")
                        result.output shouldContain expected(project.name, initial.incrementMajor(), dryRun)
                        modules.forEach { m ->
                            result.output shouldContain expected(m, initial.incrementMajor(), dryRun)
                            if (!dryRun) result.output shouldContain tagExists(m, initial.incrementMajor(), dryRun)
                        }
                    }
                }
            }

            @Suppress("DestructuringWrongName")
            context("monorepo versioning with multi-tagging${testName(release, dryRun)}") {
                it("should set initial version") {
                    // arrange
                    val initial = Semver("0.1.0")
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true)
                    Git.open(project.projectDir.toFile()).use {
                        // -> clean (no tags) repo with changes in 'root' and all modules (':core', ':foo', ':bar', ':baz')
                        project.projectDir.resolve("text.txt").createFile().writeText(faker.lorem.words())
                        it.add().addFilepattern(".").call()
                        it.commit().setMessage("First commit").call()
                        modules.forEach { m -> commit(project, it)(m) }
                    }
                    // act
                    // -> tag next minor version
                    val result = release.tag(project)(minor)(dryRun)
                    // assert
                    // -> 'root' should have initial version
                    result.output shouldContain expected(project.name, initial, dryRun)
                    modules.forEach {
                        // -> all submodules should have initial version
                        result.output shouldContain expected(it, initial, dryRun)
                        // -> ':core' submodule is NOT configured with custom tag prefix and should have 'root' tag)
                        // -> ':foo', ':bar', ':baz' submodules are configured with custom tag prefix and should have their own tags)
                        if (!dryRun) result.output shouldNotContain tagExists(it, initial, dryRun)
                    }
                }

                it("should ONLY set version for changed submodules") {
                    // arrange
                    // -> repo with initial version for each tag-prefix (v0.1.0, foo-v0.1.0, bar-v0.1.0, baz-v0.1.0)
                    val initial = Semver("0.1.0")
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true)
                    val modulesVersions = modules.map { it to initial }
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in 'root' project path
                        commit(project, it)(null)
                        // -> commit file in ':foo' submodule path
                        commit(project, it)("foo")
                    }
                    // act
                    // -> tag next minor version
                    val result = release.tag(project)(minor)(dryRun)
                    // assert
                    // -> 'root' project and ':core' submodule should have next version tag (v0.2.0)
                    //    ('root' has changes and ':core' is not configured with custom tag prefix)
                    result.output shouldContain expected(project.name, initial.incrementMinor(), dryRun)
                    result.output shouldContain expected("core", initial.incrementMinor(), dryRun)
                    // -> ':foo' submodule should have next version tag (foo-v0.2.0)
                    //    (configured with custom tag prefix and has changes)
                    result.output shouldContain expected("foo", initial.incrementMinor(), dryRun)
                    if (!dryRun) result.output shouldNotContain tagExists("foo", initial.incrementMinor(), dryRun)
                    // -> ':bar' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expected("bar", null, dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                }

                it("should set version for ROOT when non-configured submodule has changes") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v0.2.0, foo-v0.2.0, bar-v0.1.0, baz-v0.1.0)
                    val (second, initial) = Semver("0.2.0") to Semver("0.1.0")
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true)
                    val modulesVersions = listOf("core" to second, "foo" to second, "bar" to initial, "baz" to initial)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':core' submodule path
                        commit(project, it)("core")
                        // -> commit file in ':bar' submodule path
                        commit(project, it)("bar")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = release.tag(project)(minor)(dryRun)
                    // assert
                    // -> 'root' project and ':core' submodule should have next version tag (v0.3.0)
                    //    (':core' is not configured with custom tag prefix and has changes)
                    result.output shouldContain expected(project.name, second.incrementMinor(), dryRun)
                    result.output shouldContain expected("core", second.incrementMinor(), dryRun)
                    // -> ':bar' submodule should have next version tag (bar-v0.2.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expected("bar", initial.incrementMinor(), dryRun)
                    if (!dryRun) result.output shouldNotContain tagExists("bar", initial.incrementMinor(), dryRun)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                }

                it("should NOT set version for ROOT w/o changes") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v0.3.0, foo-v0.2.0, bar-v0.2.0, baz-v0.1.0)
                    val (third, second, initial) = Triple(Semver("0.3.0"), Semver("0.2.0"), Semver("0.1.0"))
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true)
                    val modulesVersions = listOf("core" to third, "foo" to second, "bar" to second, "baz" to initial)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':bar' submodule path
                        commit(project, it)("bar")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = release.tag(project)(minor)(dryRun)
                    // assert
                    // -> 'root' project and ':core' submodule should be unchanged
                    //    (we have custom tags for some submodules, and neither 'root' nor ':core' have changes)
                    result.output shouldContain expected(project.name, Semver("0.0.0"), dryRun)
                    result.output shouldContain expected("core", null, dryRun)
                    // -> ':bar' submodule should have next version tag (bar-v0.3.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expected("bar", second.incrementMinor(), dryRun)
                    if (!dryRun) result.output shouldNotContain tagExists("bar", second.incrementMinor(), dryRun)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                }

                it("should set initial version for new submodule before first stable release") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v0.3.0, foo-v0.2.0, bar-v0.2.0, baz-v0.1.0)
                    val (third, second, initial) = Triple(Semver("0.3.0"), Semver("0.2.0"), Semver("0.1.0"))
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true)
                    val modulesVersions = listOf("core" to third, "foo" to second, "bar" to second, "baz" to initial)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':bar' submodule path
                        commit(project, it)("bar")
                        // -> add new ':zoo' submodule
                        project.add(ModuleConfig("zoo", tag = object : GitTagConfig {
                            override val prefix: TagPrefix = TagPrefix("zoo-v")
                        }))
                        commit(project, it)("zoo")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = release.tag(project)(minor)(dryRun)
                    // assert
                    // -> 'root' project and ':core' submodule should have next version tag (v0.4.0)
                    //    (we have modified root settings.gradle.kts by adding a new submodule)
                    result.output shouldContain expected(project.name, third.incrementMinor(), dryRun)
                    result.output shouldContain expected("core", third.incrementMinor(), dryRun)
                    // -> ':bar' submodule should have next version tag (bar-v0.3.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expected("bar", second.incrementMinor(), dryRun)
                    if (!dryRun) result.output shouldNotContain tagExists("bar", second.incrementMinor(), dryRun)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                    // -> ':zoo' submodule should have initial version tag (bar-v0.1.0)
                    //    (configured with custom tag prefix and "default initial version in pre-stable" -> '${rootVersion.major}.1.0')
                    result.output shouldContain expected("zoo", initial, dryRun)
                    if (!dryRun) result.output shouldNotContain tagExists("zoo", initial, dryRun)
                }

                listOf(null, "core", "foo").forEach { commitIn ->
                    it("should set ALL versions to next MAJOR when committing in $commitIn") {
                        // arrange
                        // -> repo with tags for each tag-prefix (v0.3.0, foo-v0.2.0, bar-v0.3.0, baz-v0.1.0)
                        val (third, second, initial) = Triple(Semver("0.3.0"), Semver("0.2.0"), Semver("0.1.0"))
                        val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true).also {
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
                            commit(project, it)(commitIn)
                            it.commitMajor(release)
                        }
                        // act
                        // -> tag next version with MAJOR release
                        val result = release.tag(project)(major)(dryRun)
                        // assert
                        // -> 'root' and all submodules should have next MAJOR version (1.0.0)
                        //    (':core' submodule is NOT configured with custom tag prefix and should have 'root' tag prefix)
                        //    (':foo', ':bar', ':baz', ':zoo' submodules are configured with custom tag prefix and should have their own tags)
                        result.output shouldContain expected(project.name, third.incrementMajor(), dryRun)
                        modulesVersions.forEach { (m, v) -> result.output shouldContain expected(m, v.incrementMajor(), dryRun) }
                    }
                }

                it("should set initial version for new submodule after first stable release") {
                    // arrange
                    // -> repo with tags for each tag-prefix (v1.0.0, foo-v1.0.0, bar-v1.0.0, baz-v1.0.0)
                    val firstStable = Semver("1.0.0")
                    val modulesVersions = modules.map { it to firstStable }
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true)
                    Git.open(project.projectDir.toFile()).use {
                        setupInitialVersions(project, it)(modulesVersions)
                        // -> commit file in ':core' submodule path
                        commit(project, it)("core")
                        // -> commit file in ':bar' submodule path
                        commit(project, it)("bar")
                        // -> add new ':abc' submodule
                        project.add(ModuleConfig("abc", tag = object : GitTagConfig {
                            override val prefix: TagPrefix = TagPrefix("abc-v")
                        }))
                        commit(project, it)("abc")
                    }
                    // act
                    // -> tag next MINOR version
                    val result = release.tag(project)(minor)(dryRun)
                    // assert
                    // -> 'root' project and ':core' submodule should have next version tag (v1.1.0)
                    //    (':core' is not configured with custom tag prefix and has changes)
                    result.output shouldContain expected(project.name, firstStable.incrementMinor(), dryRun)
                    result.output shouldContain expected("core", firstStable.incrementMinor(), dryRun)
                    if (!dryRun) result.output shouldContain tagExists("core", firstStable.incrementMinor(), dryRun)
                    // -> ':bar' submodule should have next version tag (bar-v0.3.0)
                    //    (configured with custom tag prefix has changes)
                    result.output shouldContain expected("bar", firstStable.incrementMinor(), dryRun)
                    if (!dryRun) result.output shouldNotContain tagExists("bar", firstStable.incrementMinor(), dryRun)
                    // -> ':foo' and ':baz' submodules should be unchanged
                    //    (both are configured with custom tag prefix, and they don't have changes)
                    result.output shouldContain expected("foo", null, dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                    // -> ':abc' submodule should have initial version tag (bar-v1.0.0)
                    //    (configured with custom tag prefix and "default initial version" -> '${rootVersion.major}.0.0')
                    result.output shouldContain expected("abc", firstStable.incrementMinor(), dryRun)
                    if (!dryRun) result.output shouldNotContain tagExists("abc", firstStable.incrementMinor(), dryRun)
                }

                it("should set next version") {
                    val project = SemverKtTestProject(multiModule = true, monorepo = true, multiTag = true)
                    // Arrange
                    Git.open(project.projectDir.toFile()).use {
                        it.tag().setName("v0.1.0").call() // set initial version
                        it.tag().setName("bar-v0.2.0").call() // set initial version for 'bar' module
                        listOf("foo", "bar").forEach { module -> commit(project, it)(module) }
                        it.commitMinor(release)
                    }
                    // Act
                    val result = release.tag(project)(minor)(dryRun)
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                    result.output shouldContain expected(project.name, Semver("0.0.0"), dryRun)
                    result.output shouldContain expected("core", null, dryRun)
                    result.output shouldContain expected("foo", Semver("0.1.0"), dryRun)
                    result.output shouldContain expected("bar", Semver("0.3.0"), dryRun)
                    result.output shouldContain expected("baz", null, dryRun)
                }
            }
        } }
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

            it("should set version via -Pversion even when -Prelease -Pincrement=$keyword is used") {
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
                    args = arrayOf("tag", "-PdryRun", "-Prelease", "-Pincrement=$keyword", "-Pversion=42.0.0")
                )
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Calculated next version: 42.0.0"
            }
        }
    }

    describe("snapshot versions") {
        listOf("0.1.0", "0.1.0-rc.123", "0.1.0-rc.123+build.456").forEach { version ->
            listOf("major", "minor", "patch", "pre_release").forEach { inc ->
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
                    val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-Pincrement=$inc"))
                    // Assert
                    result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
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

                        > Task :tag
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
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain """
                    > Configure project :
                    Project test-project version: 0.2.0-SNAPSHOT

                    > Task :tag
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
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun", "-PpromoteRelease"))
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain """
                    > Configure project :
                    Project test-project version: 1.0.0-SNAPSHOT

                    > Task :tag
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
            val result = Builder.build(project = project, args = arrayOf("tag"))
            // Assert
            result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain """
                > Configure project :
                Project test-project version: 0.2.0-SNAPSHOT

                > Task :tag
                Snapshot version, not doing anything
            """.trimIndent()
            with(Git.open(project.projectDir.toFile()).tagList().call()) {
                size shouldBe 1
                first().name shouldBe "refs/tags/v0.1.0"
                last().name shouldBe "refs/tags/v0.1.0"
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
        Release.forEach { release -> DryRun.forEach { dryRun ->
            it("should return error when trying to release new version${testName(release, dryRun)} with any difference between working tree and HEAD") {
                val project = p(CleanRule.ALL)
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[minor]").call()
                    project.projectDir.resolve("untracked.txt").createFile().writeText("Hello, World!")
                }
                // Act
                val args = if (dryRun.value) arrayOf("tag", "-PdryRun") else arrayOf("tag")
                val result = if (dryRun.value) Builder.build(project = project, args = args)
                else Builder.buildAndFail(project = project, args = args)
                // Assert
                result.task(":tag")?.outcome shouldBe if (dryRun.value) TaskOutcome.SUCCESS else TaskOutcome.FAILED
                if (!dryRun) result.output shouldContain "Release with non-clean repository is not allowed"
            }
            it("should return error when trying to release new version${testName(release, dryRun)} with changes to tracked files") {
                val project = SemverKtTestProject()
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[minor]").call()
                    project.projectDir.resolve("text.txt").writeText("Hello, World!")
                }
                // Act
                val args = if (dryRun.value) arrayOf("tag", "-PdryRun") else arrayOf("tag")
                val result = if (dryRun.value) Builder.build(project = project, args = args)
                else Builder.buildAndFail(project = project, args = args)
                // Assert
                result.task(":tag")?.outcome shouldBe if (dryRun.value) TaskOutcome.SUCCESS else TaskOutcome.FAILED
                if (!dryRun) result.output shouldContain "Release with uncommitted changes is not allowed"
            }
            it("should allow releasing a new version${testName(release, dryRun)} with disabled clean rule") {
                val project = p(CleanRule.NONE)
                // Arrange
                Git.open(project.projectDir.toFile()).use {
                    it.tag().setName("v0.1.0").call() // set initial version
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[minor]").call()
                    project.projectDir.resolve("text.txt").writeText("Hello, World!")
                    project.projectDir.resolve("untracked.txt").createFile().writeText("Hello, World!")
                }
                // Act
                val args = if (dryRun.value) arrayOf("tag", "-PdryRun") else arrayOf("tag")
                val result = Builder.build(project = project, args = args)
                // Assert
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldNotContain "Release with uncommitted changes is not allowed"
            }
        } }
    }
})

val faker = faker {  }

// appends string to the test describe/context name
val testName: (Release, DryRun) -> String = { release, dryRun ->
    val s = StringBuilder()
    if (release.fromCommit) s.append(" via commit") else s.append(" via gradle property")
    if (dryRun.value) s.append(" and dryRun")
    s.toString()
}

val patch: (release: Release) -> Increment? = { if (it.fromCommit) null else Increment.PATCH }
val minor: (release: Release) -> Increment? = { if (it.fromCommit) null else Increment.MINOR }
val major: (release: Release) -> Increment? = { if (it.fromCommit) null else Increment.MAJOR }

val Git.commitMinor: (release: Release) -> Unit get() = {
    if (it.fromCommit) commit().setMessage("New commit\n\n[minor]").call()
    else commit().setMessage("New commit").call()
}

val Git.commitMajor: (release: Release) -> Unit get() = {
    if (it.fromCommit) commit().setMessage("New commit\n\n[major]").call()
    else commit().setMessage("New commit").call()
}

val tagExists: (name: String, ver: Semver?, dryRun: DryRun) -> String = { name, version, dryRun ->
    """
    > Task :$name:tag
    ${version?.let { "Calculated next version: $it" } ?: "Not doing anything"}
    ${version?.let { if (!dryRun) "Tag v$it already exists in project" else "" } ?: ""}
    """.trimIndent().trim()
}

val expected: (name: String, ver: Semver?, dryRun: DryRun) -> String = { name, version, dryRun ->
    val noop = "Not doing anything"
    when (name) {
        "test-project" -> """
            > Configure project :
            Project test-project version: $version

            > Task :tag
            ${version?.let { if (it == Semver("0.0.0")) noop else "Calculated next version: $it" } ?: noop}
        """.trimIndent().trim()
        "core" -> tagExists(name, version, dryRun)
        else -> """
            > Task :$name:tag
            ${version?.let { "Calculated next version: $it" } ?: noop}
        """.trimIndent().trim()
    }
}

val setupInitialVersions: (proj: AbstractProject, git: Git) -> (modules: List<Pair<String, Semver>>) -> Unit = { proj, git ->
    { modules ->
        proj.projectDir.resolve("text.txt").createFile().writeText(faker.lorem.words())
        git.add().addFilepattern(".").call()
        git.commit().setMessage("First commit").call()

        modules.shuffled().forEach { (m, v) ->
            val tagName = if (m == "core") "v$v" else "$m-v$v"
            // commit in 'root' together with 'core'
            if (m == "core") commit(proj, git)(null)
            commit(proj, git)(m)
            git.tag().setName(tagName).call()
        }
    }
}

/**
 * Commit to a given [module] (or in the root of the project if module is `null`) in a [project].
 */
val commit: (project: AbstractProject, git: Git) -> (module: String?) -> Unit = { proj, git ->
    { m ->
        if (m != null) {
            proj.projectDir.resolve(m)
                .resolve(if (m == "core") "src" else "${m}src").createDirectories()
                .resolve("${faker.random.nextUUID()}.txt")
                .createFile()
                .writeText(faker.lorem.words())
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Commit in $m").call()
        } else {
            proj.projectDir.resolve("${faker.random.nextUUID()}.txt")
                .createFile()
                .writeText(faker.lorem.words())
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Commit in root").call()
        }
    }
}
