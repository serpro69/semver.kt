package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.testConfiguration
import io.github.serpro69.semverkt.release.testRepo
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class RepositoryTest : DescribeSpec() {
    private val repo = GitRepository(testConfiguration)

    init {
        describe("A git repository") {
            context("log") {
                it("should contain a list of versions") {
                    repo.log().commits.mapNotNull { it.version } shouldBe listOf(
                        Semver("0.4.0"),
                        Semver("0.3.0"),
                        Semver("0.2.0"),
                        Semver("0.1.0")
                    )
                }
                it("should return last version by tag") {
                    repo.lastVersion?.simpleTagName shouldBe "v0.4.0"
                }
                it("should return a log of commits after the last version") {
                    val commits = repo.log(repo.lastVersion).commits
                    assertSoftly {
                        commits.size shouldBe 2
                        commits.first().message.title shouldBe "Commit #6"
                        commits.last().message.title shouldBe "Commit #5"
                        commits.mapNotNull { it.version } shouldBe emptyList()
                    }
                }
                it("should return full log of commits if untilTag is null") {
                    repo.log(untilTag = null).commits shouldContainExactly repo.log().commits
                }
                it("should filter commits by predicate") {
                    val commits = repo.log { it.title == "Commit #6" }.commits
                    assertSoftly {
                        commits.size shouldBe 1
                        commits.first().message.title shouldBe "Commit #6"
                    }
                }
            }
        }
    }

    override fun beforeSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
        testRepo()
    }

    override fun afterSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }
}
