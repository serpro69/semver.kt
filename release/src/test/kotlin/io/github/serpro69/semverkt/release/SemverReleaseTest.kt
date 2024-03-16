package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.PropertiesConfiguration
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import java.util.*

class SemverReleaseTest : DescribeSpec() {
    private var repo: Repository = GitRepository(testConfiguration)
    private val git: () -> Git = { Git.open(testConfiguration.git.repo.directory.toFile()) }
    private var monoRepo: Repository = GitRepository(monorepoTestConfig)
//    private val monorepoGit: () -> Git = { Git.open(monorepoTestConfig.git.repo.directory.toFile()) }
    private val semverRelease: (r: Repository) -> SemverRelease = { SemverRelease(it) }

    init {
        describe("next version Increment from commit") {
            it("should return MAJOR") {
                git().use {
                    it.addCommit("Release [major]")
                    it.addCommit("Release [pre release]")
                    it.addCommit("Release [minor]")
                    it.addCommit("Release [patch]")
                }
                semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.MAJOR
            }
            it("should return MINOR") {
                git().use {
                    it.addCommit("Release [pre release]")
                    it.addCommit("Release [minor]")
                    it.addCommit("Release [patch]")
                }
                semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.MINOR
            }
            it("should return PATCH") {
                git().use {
                    it.addCommit("Release [patch]")
                    it.addCommit("Release [pre release]")
                }
                semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.PATCH
            }
            it("should return PRE_RELEASE") {
                git().use { it.addCommit("Release [pre release]") }
                semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.PRE_RELEASE
            }
            context("should return DEFAULT") {
                it("when no keywords found") {
                    git().use { it.addCommit("Not a release") }
                    semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.DEFAULT
                }
            }
            context("should return NONE") {
                it("when HEAD points to latest release") {
                    git().use { it.addRelease(0, Semver("1.0.0")) }
                    semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.NONE
                }
                it("when HEAD points to a release tag") {
                    git().use {
                        it.addRelease(3, Semver("1.0.0"))
                        it.checkout().setName("v0.4.0").call()
                    }
                    semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.NONE
                }
            }
        }

        describe("Semver") {
            listOf(
                Semver("1.0.0-SNAPSHOT"),
                Semver("1.0.0-rc.1"),
                Semver("1.0.0")
            ).forEach { v ->
                it("increment major release after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use { it.release(Increment.MAJOR) } shouldBe Semver("2.0.0")
                }
                it("increment major snapshot after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use { it.snapshot(Increment.MAJOR) } shouldBe Semver("2.0.0-SNAPSHOT")
                }
                it("increment minor release after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use { it.release(Increment.MINOR) } shouldBe Semver("1.1.0")
                }
                it("increment minor snapshot after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use { it.snapshot(Increment.MINOR) } shouldBe Semver("1.1.0-SNAPSHOT")
                }
                it("increment patch release after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use { it.release(Increment.PATCH) } shouldBe Semver("1.0.1")
                }
                it("increment patch snapshot after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use { it.snapshot(Increment.PATCH) } shouldBe Semver("1.0.1-SNAPSHOT")
                }
            }
            it("increment pre_release") {
                git().use { git ->
                    semverRelease(repo).use {
                        git.addRelease(0, Semver("1.0.0-rc.1"))
                        git.addCommit("Test commit")
                        it.release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.2")
                        git.addRelease(0, Semver("1.0.0-rc.123"))
                        git.addCommit("Test commit")
                        it.release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.124")
                        git.addRelease(0, Semver("1.0.0"))
                        git.addCommit("Test commit")
                        it.release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
                    }
                }
            }
            it("increment pre_release snapshot") {
                git().use { git ->
                    semverRelease(repo).use {
                        git.addRelease(0, Semver("1.0.0-SNAPSHOT"))
                        git.addCommit("Test commit")
                        it.snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-SNAPSHOT")
                        git.addRelease(0, Semver("1.0.0-rc.1"))
                        git.addCommit("Test commit")
                        it.snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.2-SNAPSHOT")
                        git.addRelease(0, Semver("1.0.0-rc.123"))
                        git.addCommit("Test commit")
                        it.snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.124-SNAPSHOT")
                        git.addRelease(0, Semver("1.0.0"))
                        git.addCommit("Test commit")
                        it.snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
                    }
                }
            }
            it("increment default release") {
                git().use { git ->
                    semverRelease(repo).use {
                        git.addRelease(0, Semver("1.0.0-rc.1"))
                        git.addCommit("Test commit")
                        it.release(Increment.DEFAULT) shouldBe Semver("1.0.0-rc.2")
                        git.addRelease(0, Semver("1.0.0"))
                        git.addCommit("Test commit")
                        it.release(Increment.DEFAULT) shouldBe Semver("1.1.0")
                    }
                }
            }
            it("increment default snapshot") {
                git().use { git ->
                    semverRelease(repo).use {
                        git.addRelease(0, Semver("1.0.0-rc.1"))
                        git.addCommit("Test commit")
                        it.snapshot(Increment.DEFAULT) shouldBe Semver("1.0.0-rc.2-SNAPSHOT")
                        git.addRelease(0, Semver("1.0.0"))
                        git.addCommit("Test commit")
                        it.snapshot(Increment.DEFAULT) shouldBe Semver("1.1.0-SNAPSHOT")
                    }
                }
            }

            context("create new pre release") {
                it("snapshot version should not change") {
                    git().use {
                        it.addRelease(0, Semver("1.0.0-SNAPSHOT"))
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use {
                        it.createPreRelease(Increment.MAJOR) shouldBe Semver("1.0.0-SNAPSHOT")
                        it.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-SNAPSHOT")
                        it.createPreRelease(Increment.DEFAULT) shouldBe Semver("1.0.0-SNAPSHOT")
                    }
                }
                it("pre-release version should not change") {
                    git().use { it.addRelease(1, Semver("1.0.0-rc.3")) }
                    semverRelease(repo).use {
                        it.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.3")
                        it.createPreRelease(Increment.NONE) shouldBe Semver("1.0.0-rc.3")
                    }
                }
                it("release version should not change")  {
                    git().use {
                        it.addRelease(0, Semver("1.0.0"))
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use {
                        it.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
                        it.createPreRelease(Increment.NONE) shouldBe Semver("1.0.0")
                    }
                }
                it("first pre-release version should be created for the specified increment") {
                    git().use {
                        it.addRelease(0, Semver("1.0.0"))
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use {
                        it.createPreRelease(Increment.MAJOR) shouldBe Semver("2.0.0-rc.1")
                        it.createPreRelease(Increment.MINOR) shouldBe Semver("1.1.0-rc.1")
                        it.createPreRelease(Increment.PATCH) shouldBe Semver("1.0.1-rc.1")
                        it.createPreRelease(Increment.DEFAULT) shouldBe Semver("1.1.0-rc.1")
                    }
                }
                it("normal version should be bumped and first pre-release created") {
                    git().use {
                        it.addRelease(1, Semver("1.0.0-rc.1"))
                        it.addRelease(1, Semver("1.0.0-rc.2"))
                        it.addRelease(1, Semver("1.0.0-rc.3"))
                        it.addCommit("Test commit")
                    }
                    semverRelease(repo).use {
                        it.createPreRelease(Increment.MAJOR) shouldBe Semver("2.0.0-rc.1")
                        it.createPreRelease(Increment.MINOR) shouldBe Semver("1.1.0-rc.1")
                        it.createPreRelease(Increment.PATCH) shouldBe Semver("1.0.1-rc.1")
                        it.createPreRelease(Increment.DEFAULT) shouldBe Semver("1.1.0-rc.1")
                    }
                }
                it("initial version with pre-release should be created") {
                    val tempDir = Files.createTempDirectory("semver-test")
                    Git.init().setDirectory(tempDir.toFile()).call().use {
                        it.addCommit("Test commit")
                    }
                    val props = Properties().apply {
                        this["git.repo.directory"] = tempDir
                    }
                    SemverRelease(GitRepository(PropertiesConfiguration(props))).use { sv ->
                        sv.createPreRelease(Increment.MAJOR) shouldBe Semver("0.1.0-rc.1")
                        sv.createPreRelease(Increment.MINOR) shouldBe Semver("0.1.0-rc.1")
                        sv.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("0.1.0-rc.1")
                        sv.createPreRelease(Increment.DEFAULT) shouldBe Semver("0.1.0-rc.1")
                        sv.createPreRelease(Increment.NONE) shouldBe Semver("0.1.0-rc.1")
                    }
                    tempDir.toFile().deleteRecursively()
                }
            }

            context("promote pre-release to a release") {
                it("should promote to a release version") {
                    git().use { it.addRelease(0, Semver("1.0.0-rc.1")) }
                    semverRelease(repo).use { it.promoteToRelease() } shouldBe Semver("1.0.0")
                }
                it("should return current version if not on pre-release") {
                    git().use { it.addRelease(0, Semver("1.0.0")) }
                    semverRelease(repo).use {
                        it.promoteToRelease() shouldBe it.latestVersion(null)
                    }
                }
                it("should return null if no versions exist in the project") {
                    val tempDir = Files.createTempDirectory("semver-test")
                    Git.init().setDirectory(tempDir.toFile()).call().use {
                        it.addCommit("Test commit")
                    }
                    val props = Properties().apply {
                        this["git.repo.directory"] = tempDir
                    }
                    SemverRelease(GitRepository(PropertiesConfiguration(props))).use { sv ->
                        sv.promoteToRelease() shouldBe null
                    }
                    tempDir.toFile().deleteRecursively()
                }
            }
        }

        it("should not take invalid tag for consideration") {
            git().use {
                it.addRelease(0, Semver("1.0.0"))
                it.addCommit("Test commit")
                it.tag().setAnnotated(true).setName("x1.2.3").setForceUpdate(true).call()
                it.addCommit("Test commit")
            }
            semverRelease(repo).use { it.release(Increment.MAJOR) } shouldBe Semver("2.0.0")
        }

        describe("manual release") {
            it("should be possible to release the version manually when versions exist") {
                semverRelease(repo).use { it.release(Semver("1.0.0")) } shouldBe Semver("1.0.0")
            }
            it("should be possible to release the version manually when no versions exist") {
                val tempDir = Files.createTempDirectory("semver-test")
                Git.init().setDirectory(tempDir.toFile()).call().use {
                    it.addCommit("Test commit")
                }
                val props = Properties().apply { this["git.repo.directory"] = tempDir }
                SemverRelease(GitRepository(PropertiesConfiguration(props))).use { sv ->
                    sv.release(Semver("1.0.0")) shouldBe Semver("1.0.0")
                }
                tempDir.toFile().deleteRecursively()
            }
            it("should return null if version is less than latest version") {
                git().use { it.addRelease(0, Semver("2.0.0")) }
                semverRelease(repo).use { it.release(Semver("1.0.0")) } shouldBe null
            }
            it("should return null if version already exists") {
                semverRelease(repo).use { it.release(Semver("0.3.0")) } shouldBe null
            }
        }

        describe("monorepo project with custom-tagged submodule") {
            it("should release submodule version") {
//                semverReleaseMonorepo(monoRepo, "foo").release(Increment.MINOR) shouldBe Semver("0.3.0")
            }
            it("should release normal version") {
//                semverReleaseMonorepo(monoRepo, null).release(Increment.MINOR) shouldBe Semver("0.5.0")
            }
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
        monorepoTestConfig.git.repo.directory.toFile().deleteRecursively()
    }

    override suspend fun beforeTest(testCase: TestCase) {
        repo = GitRepository(testConfiguration)
        monoRepo = GitRepository(monorepoTestConfig)
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
