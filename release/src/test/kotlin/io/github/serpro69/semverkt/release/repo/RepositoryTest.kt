package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.testConfiguration
import io.github.serpro69.semverkt.release.testRepo
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

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
                    )
                }
                it("should return latest released version by tag") {
                    assertSoftly {
                        with(repo.latestVersionedCommit()) {
                            this?.objectId shouldNotBe null
                            this?.version shouldBe Semver("0.4.0")
                            this?.message?.title shouldBe "Next release commit"
                            this?.message?.description shouldBe listOf("Release version 0.4.0")
                        }
                    }
                }
            }
        }
    }

    override fun beforeSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }

    override fun beforeEach(testCase: TestCase) {
        testRepo()
    }

    override fun afterEach(testCase: TestCase, result: TestResult) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }
}
