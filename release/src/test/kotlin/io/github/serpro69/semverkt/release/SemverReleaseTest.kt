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
    private val semverRelease = { SemverRelease(repo) }
    private val git = { Git.open(testConfiguration.git.repo.directory.toFile()) }

    init {
        describe("next version Increment ") {
            it("should return MAJOR") {
                git().addCommit("Release [major]")
                git().addCommit("Release [pre release]")
                git().addCommit("Release [minor]")
                git().addCommit("Release [patch]")
                semverRelease().nextIncrement() shouldBe Increment.MAJOR
            }
            it("should return MINOR") {
                git().addCommit("Release [pre release]")
                git().addCommit("Release [minor]")
                git().addCommit("Release [patch]")
                semverRelease().nextIncrement() shouldBe Increment.MINOR
            }
            it("should return PATCH") {
                git().addCommit("Release [patch]")
                git().addCommit("Release [pre release]")
                semverRelease().nextIncrement() shouldBe Increment.PATCH
            }
            it("should return PRE_RELEASE") {
                git().addCommit("Release [pre release]")
                semverRelease().nextIncrement() shouldBe Increment.PRE_RELEASE
            }
            it("should return NONE") {
                git().addCommit("Not a release")
                semverRelease().nextIncrement() shouldBe Increment.DEFAULT
            }
        }

        describe("Semver") {
            listOf(
                Semver("1.0.0-SNAPSHOT"),
                Semver("1.0.0-rc.1"),
                Semver("1.0.0")
            ).forEach { v ->
                it("increment major release after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease().release(Increment.MAJOR) shouldBe Semver("2.0.0")
                }
                it("increment major snapshot after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease().snapshot(Increment.MAJOR) shouldBe Semver("2.0.0-SNAPSHOT")
                }
                it("increment minor release after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease().release(Increment.MINOR) shouldBe Semver("1.1.0")
                }
                it("increment minor snapshot after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease().snapshot(Increment.MINOR) shouldBe Semver("1.1.0-SNAPSHOT")
                }
                it("increment patch release after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease().release(Increment.PATCH) shouldBe Semver("1.0.1")
                }
                it("increment patch snapshot after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease().snapshot(Increment.PATCH) shouldBe Semver("1.0.1-SNAPSHOT")
                }
            }
            it("increment pre_release") {
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease().release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.2")
                git().addRelease(0, Semver("1.0.0-rc.123"))
                git().addCommit("Test commit")
                semverRelease().release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.124")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease().release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
            }
            it("increment pre_release snapshot") {
                git().addRelease(0, Semver("1.0.0-SNAPSHOT"))
                git().addCommit("Test commit")
                semverRelease().snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease().snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.2-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0-rc.123"))
                git().addCommit("Test commit")
                semverRelease().snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.124-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease().snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
            }
            it("increment default release") {
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease().release(Increment.DEFAULT) shouldBe Semver("1.0.0-rc.2")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease().release(Increment.DEFAULT) shouldBe Semver("1.1.0")
            }
            it("increment default snapshot") {
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease().snapshot(Increment.DEFAULT) shouldBe Semver("1.0.0-rc.2-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease().snapshot(Increment.DEFAULT) shouldBe Semver("1.1.0-SNAPSHOT")
            }
            context("create new pre release") {
                it("snapshot version should not change") {
                    git().addRelease(0, Semver("1.0.0-SNAPSHOT"))
                    git().addCommit("Test commit")
                    semverRelease().createPreRelease(Increment.MAJOR) shouldBe Semver("1.0.0-SNAPSHOT")
                }
                it("pre-release version should not change") {
                    val noChangeVer = Semver("1.0.0-rc.1")
                    git().addRelease(0, noChangeVer)
                    git().addCommit("Test commit")
                    semverRelease().createPreRelease(Increment.MAJOR) shouldBe noChangeVer
                    semverRelease().createPreRelease(Increment.PRE_RELEASE) shouldBe noChangeVer
                }
                it("first pre-release version should be created") {
                    git().addRelease(0, Semver("1.0.0"))
                    git().addCommit("Test commit")
                    semverRelease().createPreRelease(Increment.MAJOR) shouldBe Semver("2.0.0-rc.1")
                    semverRelease().createPreRelease(Increment.MINOR) shouldBe Semver("1.1.0-rc.1")
                    semverRelease().createPreRelease(Increment.PATCH) shouldBe Semver("1.0.1-rc.1")
                    semverRelease().createPreRelease(Increment.DEFAULT) shouldBe Semver("1.1.0-rc.1")
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
