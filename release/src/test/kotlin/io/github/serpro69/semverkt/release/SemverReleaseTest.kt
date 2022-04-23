package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.spec.Semver
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
            context("next version Increment ") {
                val git = { Git.open(testConfiguration.git.repo.directory.toFile()) }
                it("should return MAJOR") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [major]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [minor]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [patch]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).nextIncrement() shouldBe Increment.MAJOR
                    }
                }
                it("should return MINOR") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [minor]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [patch]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).nextIncrement() shouldBe Increment.MINOR
                    }
                }
                it("should return PATCH") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [patch]")
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).nextIncrement() shouldBe Increment.PATCH
                    }
                }
                it("should return PRE_RELEASE") {
                    git().addCommit(testConfiguration.git.repo.directory, "Release [pre release]")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).nextIncrement() shouldBe Increment.PRE_RELEASE
                    }
                }
                it("should return NONE") {
                    git().addCommit(testConfiguration.git.repo.directory, "Not a release")
                    with(semverRelease) {
                        repo.log(repo.lastVersion()).nextIncrement() shouldBe Increment.DEFAULT
                    }
                }
            }

            context("Semver") {
                with(semverRelease) {
                    it("increment major release") {
                        Semver("1.0.0").increment(Increment.MAJOR) shouldBe Semver("2.0.0")
                        Semver("1.0.0-rc.1").increment(Increment.MAJOR) shouldBe Semver("2.0.0")
                        Semver("1.0.0-SNAPSHOT").increment(Increment.MAJOR) shouldBe Semver("2.0.0")
                    }
                    it("increment major snapshot") {
                        Semver("1.0.0").increment(Increment.MAJOR, false) shouldBe Semver("2.0.0-SNAPSHOT")
                        Semver("1.0.0-rc.1").increment(Increment.MAJOR, false) shouldBe Semver("2.0.0-SNAPSHOT")
                        Semver("1.0.0-SNAPSHOT").increment(Increment.MAJOR, false) shouldBe Semver("2.0.0-SNAPSHOT")
                    }
                    it("increment minor release") {
                        Semver("1.0.0").increment(Increment.MINOR) shouldBe Semver("1.1.0")
                        Semver("1.0.0-rc.1").increment(Increment.MINOR) shouldBe Semver("1.1.0")
                        Semver("1.0.0-SNAPSHOT").increment(Increment.MINOR) shouldBe Semver("1.1.0")
                    }
                    it("increment minor snapshot") {
                        Semver("1.0.0").increment(Increment.MINOR, false) shouldBe Semver("1.1.0-SNAPSHOT")
                        Semver("1.0.0-rc.1").increment(Increment.MINOR, false) shouldBe Semver("1.1.0-SNAPSHOT")
                        Semver("1.0.0-SNAPSHOT").increment(Increment.MINOR, false) shouldBe Semver("1.1.0-SNAPSHOT")
                    }
                    it("increment patch release") {
                        Semver("1.0.0").increment(Increment.PATCH) shouldBe Semver("1.0.1")
                        Semver("1.0.0-rc.1").increment(Increment.PATCH) shouldBe Semver("1.0.1")
                        Semver("1.0.0-SNAPSHOT").increment(Increment.PATCH) shouldBe Semver("1.0.1")
                    }
                    it("increment patch snapshot") {
                        Semver("1.0.0").increment(Increment.PATCH, false) shouldBe Semver("1.0.1-SNAPSHOT")
                        Semver("1.0.0-rc.1").increment(Increment.PATCH, false) shouldBe Semver("1.0.1-SNAPSHOT")
                        Semver("1.0.0-SNAPSHOT").increment(Increment.PATCH, false) shouldBe Semver("1.0.1-SNAPSHOT")
                    }
                    it("increment pre_release") {
                        Semver("1.0.0").increment(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
                        Semver("1.0.0-rc.0").increment(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.1")
                        Semver("1.0.0-rc.123").increment(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.124")
                        Semver("1.0.0-foo.bar.69").increment(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-foo.bar.70")
                    }
                    it("increment pre_release snapshot") {
                        Semver("1.0.0").increment(Increment.PRE_RELEASE, false) shouldBe Semver("1.0.0-SNAPSHOT")
                        Semver("1.0.0-rc.0").increment(
                            Increment.PRE_RELEASE,
                            false
                        ) shouldBe Semver("1.0.0-rc.1-SNAPSHOT")
                        Semver("1.0.0-rc.123").increment(
                            Increment.PRE_RELEASE,
                            false
                        ) shouldBe Semver("1.0.0-rc.124-SNAPSHOT")
                        Semver("1.0.0-foo.bar.69").increment(
                            Increment.PRE_RELEASE,
                            false
                        ) shouldBe Semver("1.0.0-foo.bar.70-SNAPSHOT")
                    }
                    it("increment default release") {
                        Semver("1.0.0").increment(Increment.DEFAULT) shouldBe Semver("1.1.0")
                        Semver("1.0.0-rc.1").increment(Increment.DEFAULT) shouldBe Semver("1.0.0-rc.2")
                    }
                    it("increment default snapshot") {
                        Semver("1.0.0").increment(Increment.DEFAULT, false) shouldBe Semver("1.1.0-SNAPSHOT")
                        Semver("1.0.0-rc.1").increment(Increment.DEFAULT, false) shouldBe Semver("1.0.0-rc.2-SNAPSHOT")
                    }
                    it("create new pre release") {
                        Semver("1.0.0").createPreRelease() shouldBe Semver("1.1.0-rc.1")
                        Semver("1.0.0-rc.1").createPreRelease() shouldBe Semver("1.0.0-rc.1")
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
