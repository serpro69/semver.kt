package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.addCommit
import io.github.serpro69.semverkt.release.addRelease
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.release.monorepoTestConfig
import io.github.serpro69.semverkt.release.testConfiguration
import io.github.serpro69.semverkt.release.testMonoRepo
import io.github.serpro69.semverkt.release.testRepo
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.api.Git
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class RepositoryTest : DescribeSpec() {

    private val repo: Repository = GitRepository(testConfiguration)
    private val git = { Git.open(testConfiguration.git.repo.directory.toFile()) }
    private val monoRepo = GitRepository(monorepoTestConfig)
    private val monoGit = { Git.open(monorepoTestConfig.git.repo.directory.toFile()) }
    private val monoFooTagConfig = requireNotNull(monorepoTestConfig.monorepo.modules.first { it.path == "foo" }.tag)
    private val tagPrefix = TagPrefix("foo-v")

    init {
        describe("log") {
            it("should contain a list of versions") {
                val expected = listOf(
                    Semver("0.4.0"),
                    Semver("0.3.0"),
                    Semver("0.2.0"),
                    Semver("0.1.0")
                )
                repo.use { r ->
                    r.log()
                        .mapNotNull { it.tag }
                        .map { semver(testConfiguration.git.tag.prefix)(it) } shouldBe expected
                }
            }
            it("should return last version by tag") {
                repo.use { it.latestVersionTag()?.simpleTagName } shouldBe "v0.4.0"
            }
            it("should return a log of commits after the last version") {
                val commits = repo.use { it.log(repo.latestVersionTag()) }
                assertSoftly {
                    commits.size shouldBe 2
                    commits.first().message.title shouldBe "Commit #6"
                    commits.last().message.title shouldBe "Commit #5"
                    commits.mapNotNull { it.tag } shouldBe emptyList()
                }
            }
            it("should return full log of commits if untilTag is null") {
                repo.use { it.log(untilTag = null) shouldContainExactly it.log() }
            }
            it("should filter commits by predicate") {
                val commits = repo.use { r -> r.log { it.title == "Commit #6" } }
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
                monoRepo.use { r ->
                    r.log(tagPrefix = tagPrefix)
                        .mapNotNull { it.tag }
                        .map { semver(monoFooTagConfig.prefix)(it) } shouldBe expected
                }
            }
            it("should return last version by tag") {
                monoRepo.use { it.latestVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v0.2.0"
            }
            it("should return a log of commits after the last version") {
                val commits = monoRepo.use { it.log(it.latestVersionTag(), tagPrefix) }
                assertSoftly {
                    commits.size shouldBe 2
                    commits.first().message.title shouldBe "Commit #6"
                    commits.last().message.title shouldBe "Commit #5"
                    commits.mapNotNull { it.tag } shouldBe emptyList()
                }
            }
            it("should return full log of commits if untilTag is null") {
                monoRepo.use {
                    it.log(untilTag = null, tagPrefix) shouldContainExactly it.log(tagPrefix = tagPrefix)
                }
            }
            it("should filter commits by predicate") {
                val commits = monoRepo.use { r ->
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
                repo.use { it.latestVersionTag()?.simpleTagName } shouldBe "v0.4.0"
            }
            it("should return last version tag - unordered") {
                git().use {
                    it.addRelease(3, Semver("3.0.0"))
                    it.addRelease(3, Semver("2.0.0"))
                    it.addRelease(3, Semver("1.0.0"))
                }
                repo.use { it.latestVersionTag()?.simpleTagName } shouldBe "v3.0.0"
            }
        }

        describe("latest version tag with custom prefix") {
            it("should return last version tag - ordered") {
                monoRepo.use { it.latestVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v0.2.0"
            }
            it("should return last version - unordered") {
                monoGit().use {
                    it.addRelease(3, Semver("3.0.0"), submodule = "foo")
                    it.addRelease(3, Semver("2.0.0"), submodule = "foo")
                    it.addRelease(3, Semver("1.0.0"), submodule = "foo")
                }
                monoRepo.use { it.latestVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v3.0.0"
            }
        }

        describe("HEAD version tag") {
            it("should return annotated tag version at HEAD") {
                git().use { it.addRelease(1, Semver("1.0.0"), annotated = true) }
                repo.use { it.headVersionTag()?.simpleTagName } shouldBe "v1.0.0"
            }
            it("should return tag version at HEAD") {
                git().use { it.addRelease(1, Semver("1.0.0"), annotated = false) }
                repo.use { it.headVersionTag()?.simpleTagName } shouldBe "v1.0.0"
            }
            it("should return null if HEAD has no tags") {
                git().use { it.addCommit("Test commit without release") }
                repo.use { it.headVersionTag() } shouldBe null
            }
            it("should return null if HEAD has no tags with default prefix") {
                monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = false, submodule = "foo") }
                repo.use { it.headVersionTag(tagPrefix) } shouldBe null
            }
        }

        describe("HEAD version tag with custom tag prefix") {
            it("should return annotated tag version at HEAD") {
                monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = true, submodule = "foo") }
                monoRepo.use { it.headVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v1.0.0"
            }
            it("should return tag version at HEAD") {
                monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = false, submodule = "foo") }
                monoRepo.use { it.headVersionTag(tagPrefix)?.simpleTagName } shouldBe "foo-v1.0.0"
            }
            it("should return null if HEAD has no tags") {
                monoGit().use { it.addCommit("Test commit without release") }
                monoRepo.use { it.headVersionTag(tagPrefix) } shouldBe null
            }
            it("should return null if HEAD has no tags with matching prefix") {
                monoGit().use { it.addRelease(1, Semver("1.0.0"), annotated = true) }
                monoRepo.use { it.headVersionTag(tagPrefix) } shouldBe null
            }
        }

        describe("diff") {
            it("should contain a list of changed files between HEAD and last version") {
                git().use {
                    it.tag().setName("v0.5.0").call()
                    it.addCommit("Commit #7", fileName = "testfile")
                    it.addCommit("Commit #8", fileName = "testfile2")
                }
                val diff = repo.use { it.diff(it.head().name, it.latestVersionTag()?.name!!) }
                assertSoftly {
                    diff.size shouldBe 2
                    diff.first().oldPath shouldBe "testfile.txt"
                    diff.last().oldPath shouldBe "testfile2.txt"
                }
            }
        }

        describe("isClean") {
            it("should return true with no differences between HEAD and index") {
                git().use { git ->
                    repo.config.git.repo.directory.resolve("foo").createDirectories().also { p ->
                        p.resolve("test.txt").createFile().writeText("hello world")
                        git.add().addFilepattern(".").call()
                        git.commit().setMessage("test commit").call()
                    }
                }
                repo.use { it.isClean() } shouldBe true
            }
            it("should return false with untracked files") {
                git().use { git ->
                    repo.config.git.repo.directory.resolve("foo").createDirectories().also { p ->
                        p.resolve("test.txt").createFile().writeText("hello world")
                        git.add().addFilepattern(".").call()
                        git.commit().setMessage("test commit").call()
                        p.resolve("untracked.text").createFile()
                    }
                }
                repo.use { it.isClean() } shouldBe false
            }
            it("should return false with uncommitted staged changes") {
                git().use {
                    repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                    it.add().addFilepattern("test.txt").call()
                }
                repo.use { it.isClean() } shouldBe false
            }
            it("should return false with uncommitted and unstaged changes") {
                git().use {
                    // arrange
                    repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                    it.add().addFilepattern("test.txt").call()
                    it.commit().setMessage("test").call()
                    // change file
                    repo.config.git.repo.directory.resolve("test.txt").writeText("change file contents")
                }
                repo.use { it.isClean() } shouldBe false
            }
        }

        describe("hasUncommittedChanges") {
            it("should return true with no uncommitted changes") {
                git().use { git ->
                    repo.config.git.repo.directory.resolve("foo").createDirectories().also { p ->
                        p.resolve("test.txt").createFile().writeText("hello world")
                        git.add().addFilepattern(".").call()
                        git.commit().setMessage("test commit").call()
                        p.resolve("untracked.text").createFile()
                    }
                }
                repo.use { it.hasUncommittedChanges() } shouldBe false
            }
            it("should return false with uncommitted staged changes") {
                git().use {
                    repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                    it.add().addFilepattern("test.txt").call()
                }
                repo.use { it.hasUncommittedChanges() } shouldBe true
            }
            it("should return false with uncommitted and unstaged changes") {
                git().use {
                    // arrange
                    repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                    it.add().addFilepattern("test.txt").call()
                    it.commit().setMessage("test").call()
                    // change file
                    repo.config.git.repo.directory.resolve("test.txt").writeText("change file contents")
                }
                repo.use { it.hasUncommittedChanges() } shouldBe true
            }
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
        monorepoTestConfig.git.repo.directory.toFile().deleteRecursively()
    }

    override suspend fun beforeTest(testCase: TestCase) {
        testRepo()
        testMonoRepo()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        repo.close()
        monoRepo.close()
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
        monorepoTestConfig.git.repo.directory.toFile().deleteRecursively()
    }

    override fun afterSpec(f: suspend (Spec) -> Unit) {
        repo.close()
        monoRepo.close()
    }
}
