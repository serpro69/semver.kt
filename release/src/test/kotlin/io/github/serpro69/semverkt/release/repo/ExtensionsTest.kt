package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.testConfiguration
import io.github.serpro69.semverkt.release.testRepo
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe

class ExtensionsTest : DescribeSpec() {
    private val repo = GitRepository(testConfiguration)

    init {
        describe("repo extension functions") {
            it("semver() fun") {
                semver(testConfiguration.git.tag)(repo.latestVersionTag()!!) shouldBe Semver("0.4.0")
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
