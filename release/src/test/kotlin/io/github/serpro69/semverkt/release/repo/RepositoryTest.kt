package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.testConfiguration
import io.github.serpro69.semverkt.release.testRepo
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
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
                    )
                }
            }
        }
    }

    override fun beforeEach(testCase: TestCase) {
        testRepo()
    }

    override fun afterEach(testCase: TestCase, result: TestResult) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }
}
