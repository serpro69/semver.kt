package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.TestFixtures
import io.github.serpro69.semverkt.release.addCommit
import io.github.serpro69.semverkt.release.addRelease
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class RepositoryTest : TestFixtures({ test ->

    val monoFooTagConfig = requireNotNull(test.monorepoTestConfig.monorepo.modules.first { it.path == "foo" }.tag)
    val tagPrefix = TagPrefix("foo-v")

    describe("log") {
        it("should contain a list of versions") {
            val expected = listOf(
                Semver("0.4.0"),
                Semver("0.3.0"),
                Semver("0.2.0"),
                Semver("0.1.0")
            )
            test.repo.use { r ->
                r.log()
                    .mapNotNull { it.tag }
                    .map { semver(test.testConfiguration.git.tag.prefix)(it) } shouldBe expected
            }
        }
        it("should return last version by tag") {
            test.repo.use { it.latestVersionTag()?.simpleTagName } shouldBe "v0.4.0"
        }
        it("should return a log of commits after the last version") {
            val commits = test.repo.use { it.log(test.repo.latestVersionTag()) }
            assertSoftly {
                commits.size shouldBe 2
                commits.first().message.title shouldBe "Commit #6"
                commits.last().message.title shouldBe "Commit #5"
                commits.mapNotNull { it.tag } shouldBe emptyList()
            }
        }
        it("should return full log of commits if untilTag is null") {
            test.repo.use { it.log(untilTag = null) shouldContainExactly it.log() }
        }
        it("should filter commits by predicate") {
            val commits = test.repo.use { r -> r.log { it.title == "Commit #6" } }
            assertSoftly {
                commits.size shouldBe 1
                commits.first().message.title shouldBe "Commit #6"
            }
        }
    }

    describe("log with custom tag prefix") {
        it("should contain a list of versions") {
            val expected = listOf(
                Semver("0.2.0"),
                Semver("0.1.0"),
            )
            test.monoRepo.use { r ->
                r.log(tagPrefix = tagPrefix)
                    .mapNotNull { it.tag }
                    .map { semver(monoFooTagConfig.prefix)(it) } shouldBe expected
            }
        }
        it("should return last version by tag") {
            test.monoRepo.use { it.latestVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v0.2.0"
        }
        it("should return a log of commits after the last version") {
            val commits = test.monoRepo.use { it.log(it.latestVersionTag(), tagPrefix) }
            assertSoftly {
                commits.size shouldBe 2
                commits.first().message.title shouldBe "Commit #6"
                commits.last().message.title shouldBe "Commit #5"
                commits.mapNotNull { it.tag } shouldBe emptyList()
            }
        }
        it("should return full log of commits if untilTag is null") {
            test.monoRepo.use {
                it.log(untilTag = null, tagPrefix) shouldContainExactly it.log(tagPrefix = tagPrefix)
            }
        }
        it("should filter commits by predicate") {
            val commits = test.monoRepo.use { r ->
                r.log(tagPrefix = tagPrefix) { it.title == "Commit #6" }
            }
            assertSoftly {
                commits.size shouldBe 1
                commits.first().message.title shouldBe "Commit #6"
            }
        }
    }

    describe("latest version tag") {
        it("should return last version tag - ordered") {
            test.repo.use { it.latestVersionTag()?.simpleTagName } shouldBe "v0.4.0"
        }
        it("should return last version tag - unordered") {
            test.git().use {
                it.addRelease(3, Semver("3.0.0"))
                it.addRelease(3, Semver("2.0.0"))
                it.addRelease(3, Semver("1.0.0"))
            }
            test.repo.use { it.latestVersionTag()?.simpleTagName } shouldBe "v3.0.0"
        }
    }

    describe("latest version tag with custom prefix") {
        it("should return last version tag - ordered") {
            test.monoRepo.use { it.latestVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v0.2.0"
        }
        it("should return last version - unordered") {
            test.monoGit().use {
                it.addRelease(3, Semver("3.0.0"), submodule = "foo")
                it.addRelease(3, Semver("2.0.0"), submodule = "foo")
                it.addRelease(3, Semver("1.0.0"), submodule = "foo")
            }
            test.monoRepo.use { it.latestVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v3.0.0"
        }
    }

    describe("HEAD version tag") {
        it("should return annotated tag version at HEAD") {
            test.git().use { it.addRelease(1, Semver("1.0.0"), annotated = true) }
            test.repo.use { it.headVersionTag()?.simpleTagName } shouldBe "v1.0.0"
        }
        it("should return tag version at HEAD") {
            test.git().use { it.addRelease(1, Semver("1.0.0"), annotated = false) }
            test.repo.use { it.headVersionTag()?.simpleTagName } shouldBe "v1.0.0"
        }
        it("should return null if HEAD has no tags") {
            test.git().use { it.addCommit("Test commit without release") }
            test.repo.use { it.headVersionTag() } shouldBe null
        }
        it("should return null if HEAD has no tags with default prefix") {
            test.monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = false, submodule = "foo") }
            test.repo.use { it.headVersionTag(tagPrefix) } shouldBe null
        }
    }

    describe("HEAD version tag with custom tag prefix") {
        it("should return annotated tag version at HEAD") {
            test.monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = true, submodule = "foo") }
            test.monoRepo.use { it.headVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v1.0.0"
        }
        it("should return tag version at HEAD") {
            test.monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = false, submodule = "foo") }
            test.monoRepo.use { it.headVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v1.0.0"
        }
        it("should return null if HEAD has no tags") {
            test.monoGit().use { it.addCommit("Test commit without release") }
            test.monoRepo.use { it.headVersionTag(tagPrefix) } shouldBe null
        }
        it("should return null if HEAD has no tags with matching prefix") {
            test.monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = true) }
            test.monoRepo.use { it.headVersionTag(tagPrefix) } shouldBe null
        }
    }

    describe("diff") {
        it("should contain a list of changed files between HEAD and last version") {
            test.git().use {
                it.tag().setName("v0.5.0").call()
                it.addCommit("Commit #7", fileName = "testfile")
                it.addCommit("Commit #8", fileName = "testfile2")
            }
            val diff = test.repo.use { it.diff(it.head().name, it.latestVersionTag()?.name!!) }
            assertSoftly {
                diff.size shouldBe 2
                diff.first().oldPath shouldBe "testfile.txt"
                diff.last().oldPath shouldBe "testfile2.txt"
            }
        }
    }

    describe("isClean") {
        it("should return true with no differences between HEAD and index") {
            test.git().use { git ->
                test.repo.config.git.repo.directory.resolve("foo").createDirectories().also { p ->
                    p.resolve("test.txt").createFile().writeText("hello world")
                    git.add().addFilepattern(".").call()
                    git.commit().setMessage("test commit").call()
                }
            }
            test.repo.use { it.isClean() } shouldBe true
        }
        it("should return false with untracked files") {
            test.git().use { git ->
                test.repo.config.git.repo.directory.resolve("foo").createDirectories().also { p ->
                    p.resolve("test.txt").createFile().writeText("hello world")
                    git.add().addFilepattern(".").call()
                    git.commit().setMessage("test commit").call()
                    p.resolve("untracked.text").createFile()
                }
            }
            test.repo.use { it.isClean() } shouldBe false
        }
        it("should return false with uncommitted staged changes") {
            test.git().use {
                test.repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                it.add().addFilepattern("test.txt").call()
            }
            test.repo.use { it.isClean() } shouldBe false
        }
        it("should return false with uncommitted and unstaged changes") {
            test.git().use {
                // arrange
                test.repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                it.add().addFilepattern("test.txt").call()
                it.commit().setMessage("test").call()
                // change file
                test.repo.config.git.repo.directory.resolve("test.txt").writeText("change file contents")
            }
            test.repo.use { it.isClean() } shouldBe false
        }
    }

    describe("hasUncommittedChanges") {
        it("should return true with no uncommitted changes") {
            test.git().use { git ->
                test.repo.config.git.repo.directory.resolve("foo").createDirectories().also { p ->
                    p.resolve("test.txt").createFile().writeText("hello world")
                    git.add().addFilepattern(".").call()
                    git.commit().setMessage("test commit").call()
                    p.resolve("untracked.text").createFile()
                }
            }
            test.repo.use { it.hasUncommittedChanges() } shouldBe false
        }
        it("should return false with uncommitted staged changes") {
            test.git().use {
                test.repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                it.add().addFilepattern("test.txt").call()
            }
            test.repo.use { it.hasUncommittedChanges() } shouldBe true
        }
        it("should return false with uncommitted and unstaged changes") {
            test.git().use {
                // arrange
                test.repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                it.add().addFilepattern("test.txt").call()
                it.commit().setMessage("test").call()
                // change file
                test.repo.config.git.repo.directory.resolve("test.txt").writeText("change file contents")
            }
            test.repo.use { it.hasUncommittedChanges() } shouldBe true
        }
    }
})
