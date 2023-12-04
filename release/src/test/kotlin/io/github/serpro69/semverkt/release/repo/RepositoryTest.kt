package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.addCommit
import io.github.serpro69.semverkt.release.addRelease
import io.github.serpro69.semverkt.release.testConfiguration
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

    init {
        describe("A git repository") {

            context("log") {
                it("should contain a list of versions") {
                    val expected = listOf(
                        Semver("0.4.0"),
                        Semver("0.3.0"),
                        Semver("0.2.0"),
                        Semver("0.1.0")
                    )
                    repo.log()
                        .mapNotNull { it.tag }
                        .map { semver(testConfiguration.git.tag)(it) } shouldBe expected
                }
                it("should return last version by tag") {
                    repo.latestVersionTag()?.simpleTagName shouldBe "v0.4.0"
                }
                it("should return a log of commits after the last version") {
                    val commits = repo.log(repo.latestVersionTag())
                    assertSoftly {
                        commits.size shouldBe 2
                        commits.first().message.title shouldBe "Commit #6"
                        commits.last().message.title shouldBe "Commit #5"
                        commits.mapNotNull { it.tag } shouldBe emptyList()
                    }
                }
                it("should return full log of commits if untilTag is null") {
                    repo.log(untilTag = null) shouldContainExactly repo.log()
                }
                it("should filter commits by predicate") {
                    val commits = repo.log { it.title == "Commit #6" }
                    assertSoftly {
                        commits.size shouldBe 1
                        commits.first().message.title shouldBe "Commit #6"
                    }
                }
            }
            context("last version") {
                it("should return last version - ordered") {
                    repo.latestVersionTag()?.simpleTagName shouldBe "v0.4.0"
                }
                it("should return last version - unordered") {
                    git().use {
                        it.addRelease(3, Semver("3.0.0"))
                        it.addRelease(3, Semver("2.0.0"))
                        it.addRelease(3, Semver("1.0.0"))
                    }
                    repo.latestVersionTag()?.simpleTagName shouldBe "v3.0.0"
                }
            }
            context("HEAD version") {
                it("should return annotated tag version at HEAD") {
                    git().use { it.addRelease(1, Semver("1.0.0"), annotated = true) }
                    repo.headVersionTag()?.simpleTagName shouldBe "v1.0.0"
                }
                it("should return tag version at HEAD") {
                    git().use { it.addRelease(1, Semver("1.0.0"), annotated = false) }
                    repo.headVersionTag()?.simpleTagName shouldBe "v1.0.0"
                }
                it("should return null if HEAD has no versions") {
                    git().use { it.addCommit("Test commit without release") }
                    repo.headVersionTag() shouldBe null
                }
            }
            context("diff") {
                it("should contain a list of changed files between HEAD and last version") {
                    git().use {
                        it.tag().setName("v0.5.0").call()
                        it.addCommit("Commit #7", fileName = "testfile")
                        it.addCommit("Commit #8", fileName = "testfile2")
                    }
                    val diff = repo.diff(repo.head().name, repo.latestVersionTag()?.name!!)
                    assertSoftly {
                        diff.size shouldBe 2
                        diff.first().oldPath shouldBe "testfile.txt"
                        diff.last().oldPath shouldBe "testfile2.txt"
                    }
                }
            }
            context("isClean") {
                it("should return true with no uncommitted changes in the repo") {
                    git().use { git ->
                        repo.config.git.repo.directory.resolve("foo").createDirectories().also { p ->
                                p.resolve("test.txt").createFile().writeText("hello world")
                                git.add().addFilepattern(".").call()
                                git.commit().setMessage("test commit").call()
                                p.resolve("untracked.text").createFile()
                            }
                    }
                    repo.isClean() shouldBe true
                }
                it("should return false with uncommitted staged changes") {
                    git().use {
                        repo.config.git.repo.directory.resolve("test.txt").createFile().writeText("hello world")
                        it.add().addFilepattern("test.txt").call()
                    }
                    repo.isClean() shouldBe false
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
                    repo.isClean() shouldBe false
                }
            }
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }

    override suspend fun beforeTest(testCase: TestCase) {
        testRepo()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        repo.close()
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }
}
