package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Repository
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.api.Git

class SemverReleaseTest : DescribeSpec() {
    private var repo: Repository = GitRepository(testConfiguration)

    init {
        describe("Semantic release") {
            val semverRelease = SemverRelease(testConfiguration)
            context("A version Increment") {
                val git = { Git.open(testConfiguration.git.repo.directory.toFile()) }
                it("should return MAJOR") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [major]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [minor]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [patch]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).computeNextIncrement() shouldBe Increment.MAJOR
                    }
                }
                it("should return MINOR") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [minor]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [patch]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).computeNextIncrement() shouldBe Increment.MINOR
                    }
                }
                it("should return PATCH") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [patch]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).computeNextIncrement() shouldBe Increment.PATCH
                    }
                }
                it("should return PRE_RELEASE") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).computeNextIncrement() shouldBe Increment.PRE_RELEASE
                    }
                }
                it("should return NONE") {
                    git().addCommit(testConfiguration.git.repo.directory, "Not a release")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).computeNextIncrement() shouldBe Increment.NONE
                    }
                }
            }
        }
    }

    override fun beforeSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }

    override fun beforeTest(testCase: TestCase) {
        repo = GitRepository(testConfiguration)
        testRepo()
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
    }
}
