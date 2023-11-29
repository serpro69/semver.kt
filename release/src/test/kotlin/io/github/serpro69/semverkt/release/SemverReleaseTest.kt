package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.PropertiesConfiguration
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.core.config.configuration
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
    private val monorepoGit: () -> Git = { Git.open(monorepoTestConfig.git.repo.directory.toFile()) }
    private val semverRelease: (r: Repository) -> SemverRelease = { SemverRelease(it) }

    init {
        describe("next version Increment from commit") {
            it("should return MAJOR") {
                git().addCommit("Release [major]")
                git().addCommit("Release [pre release]")
                git().addCommit("Release [minor]")
                git().addCommit("Release [patch]")
                semverRelease(repo).nextIncrement() shouldBe Increment.MAJOR
            }
            it("should return MINOR") {
                git().addCommit("Release [pre release]")
                git().addCommit("Release [minor]")
                git().addCommit("Release [patch]")
                semverRelease(repo).nextIncrement() shouldBe Increment.MINOR
            }
            it("should return PATCH") {
                git().addCommit("Release [patch]")
                git().addCommit("Release [pre release]")
                semverRelease(repo).nextIncrement() shouldBe Increment.PATCH
            }
            it("should return PRE_RELEASE") {
                git().addCommit("Release [pre release]")
                semverRelease(repo).nextIncrement() shouldBe Increment.PRE_RELEASE
            }
            context("should return DEFAULT") {
                it("when no keywords found") {
                    git().addCommit("Not a release")
                    semverRelease(repo).nextIncrement() shouldBe Increment.DEFAULT
                }
            }
            context("should return NONE") {
                it("when HEAD points to latest release") {
                    git().addRelease(0, Semver("1.0.0"))
                    semverRelease(repo).nextIncrement() shouldBe Increment.NONE
                }
                it("when HEAD points to a release tag") {
                    git().addRelease(3, Semver("1.0.0"))
                    git().checkout().setName("v0.4.0").call()
                    semverRelease(repo).nextIncrement() shouldBe Increment.NONE
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
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease(repo).release(Increment.MAJOR) shouldBe Semver("2.0.0")
                }
                it("increment major snapshot after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease(repo).snapshot(Increment.MAJOR) shouldBe Semver("2.0.0-SNAPSHOT")
                }
                it("increment minor release after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease(repo).release(Increment.MINOR) shouldBe Semver("1.1.0")
                }
                it("increment minor snapshot after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease(repo).snapshot(Increment.MINOR) shouldBe Semver("1.1.0-SNAPSHOT")
                }
                it("increment patch release after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease(repo).release(Increment.PATCH) shouldBe Semver("1.0.1")
                }
                it("increment patch snapshot after $v") {
                    git().addRelease(0, v)
                    git().addCommit("Test commit")
                    semverRelease(repo).snapshot(Increment.PATCH) shouldBe Semver("1.0.1-SNAPSHOT")
                }
            }
            it("increment pre_release") {
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease(repo).release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.2")
                git().addRelease(0, Semver("1.0.0-rc.123"))
                git().addCommit("Test commit")
                semverRelease(repo).release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.124")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease(repo).release(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
            }
            it("increment pre_release snapshot") {
                git().addRelease(0, Semver("1.0.0-SNAPSHOT"))
                git().addCommit("Test commit")
                semverRelease(repo).snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease(repo).snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.2-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0-rc.123"))
                git().addCommit("Test commit")
                semverRelease(repo).snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.124-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease(repo).snapshot(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
            }
            it("increment default release") {
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease(repo).release(Increment.DEFAULT) shouldBe Semver("1.0.0-rc.2")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease(repo).release(Increment.DEFAULT) shouldBe Semver("1.1.0")
            }
            it("increment default snapshot") {
                git().addRelease(0, Semver("1.0.0-rc.1"))
                git().addCommit("Test commit")
                semverRelease(repo).snapshot(Increment.DEFAULT) shouldBe Semver("1.0.0-rc.2-SNAPSHOT")
                git().addRelease(0, Semver("1.0.0"))
                git().addCommit("Test commit")
                semverRelease(repo).snapshot(Increment.DEFAULT) shouldBe Semver("1.1.0-SNAPSHOT")
            }

            context("create new pre release") {
                it("snapshot version should not change") {
                    git().addRelease(0, Semver("1.0.0-SNAPSHOT"))
                    git().addCommit("Test commit")
                    semverRelease(repo).createPreRelease(Increment.MAJOR) shouldBe Semver("1.0.0-SNAPSHOT")
                    semverRelease(repo).createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-SNAPSHOT")
                    semverRelease(repo).createPreRelease(Increment.DEFAULT) shouldBe Semver("1.0.0-SNAPSHOT")
                }
                it("pre-release version should not change") {
                    git().addRelease(1, Semver("1.0.0-rc.3"))
                    semverRelease(repo).createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.3")
                    semverRelease(repo).createPreRelease(Increment.NONE) shouldBe Semver("1.0.0-rc.3")
                }
                it("release version should not change") {
                    git().addRelease(0, Semver("1.0.0"))
                    git().addCommit("Test commit")
                    semverRelease(repo).createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
                    semverRelease(repo).createPreRelease(Increment.NONE) shouldBe Semver("1.0.0")
                }
                it("first pre-release version should be created for the specified increment") {
                    git().addRelease(0, Semver("1.0.0"))
                    git().addCommit("Test commit")
                    semverRelease(repo).createPreRelease(Increment.MAJOR) shouldBe Semver("2.0.0-rc.1")
                    semverRelease(repo).createPreRelease(Increment.MINOR) shouldBe Semver("1.1.0-rc.1")
                    semverRelease(repo).createPreRelease(Increment.PATCH) shouldBe Semver("1.0.1-rc.1")
                    semverRelease(repo).createPreRelease(Increment.DEFAULT) shouldBe Semver("1.1.0-rc.1")
                }
                it("normal version should be bumped and first pre-release created") {
                    git().addRelease(1, Semver("1.0.0-rc.1"))
                    git().addRelease(1, Semver("1.0.0-rc.2"))
                    git().addRelease(1, Semver("1.0.0-rc.3"))
                    git().addCommit("Test commit")
                    semverRelease(repo).createPreRelease(Increment.MAJOR) shouldBe Semver("2.0.0-rc.1")
                    semverRelease(repo).createPreRelease(Increment.MINOR) shouldBe Semver("1.1.0-rc.1")
                    semverRelease(repo).createPreRelease(Increment.PATCH) shouldBe Semver("1.0.1-rc.1")
                    semverRelease(repo).createPreRelease(Increment.DEFAULT) shouldBe Semver("1.1.0-rc.1")
                }
                it("initial version with pre-release should be created") {
                    val tempDir = Files.createTempDirectory("semver-test")
                    val git = Git.init().setGitDir(tempDir.toFile()).call()
                    git.addCommit("Test commit")
                    val props = Properties().apply {
                        this["git.repo.directory"] = tempDir
                    }
                    val sv = SemverRelease(GitRepository(PropertiesConfiguration(props)))
                    sv.createPreRelease(Increment.MAJOR) shouldBe Semver("0.1.0-rc.1")
                    sv.createPreRelease(Increment.MINOR) shouldBe Semver("0.1.0-rc.1")
                    sv.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("0.1.0-rc.1")
                    sv.createPreRelease(Increment.DEFAULT) shouldBe Semver("0.1.0-rc.1")
                    sv.createPreRelease(Increment.NONE) shouldBe Semver("0.1.0-rc.1")
                    tempDir.toFile().deleteRecursively()
                }
            }

            context("promote pre-release to a release") {
                it("should promote to a release version") {
                    git().addRelease(0, Semver("1.0.0-rc.1"))
                    semverRelease(repo).promoteToRelease() shouldBe Semver("1.0.0")
                }
                it("should return current version if not on pre-release") {
                    git().addRelease(0, Semver("1.0.0"))
                    with(semverRelease(repo)) {
                        promoteToRelease() shouldBe latestVersion()
                    }
                }
                it("should return null if no versions exist in the project") {
                    val tempDir = Files.createTempDirectory("semver-test")
                    val git = Git.init().setGitDir(tempDir.toFile()).call()
                    git.addCommit("Test commit")
                    val props = Properties().apply {
                        this["git.repo.directory"] = tempDir
                    }
                    val sv = SemverRelease(GitRepository(PropertiesConfiguration(props)))
                    sv.promoteToRelease() shouldBe null
                    tempDir.toFile().deleteRecursively()
                }
            }
        }

        it("should not take invalid tag for consideration") {
            with(git()) {
                addRelease(0, Semver("1.0.0"))
                addCommit("Test commit")
                tag().setAnnotated(true).setName("x1.2.3").setForceUpdate(true).call()
                addCommit("Test commit")
            }
            semverRelease(repo).release(Increment.MAJOR) shouldBe Semver("2.0.0")
        }

        describe("manual release") {
            it("should be possible to release the version manually when versions exist") {
                semverRelease(repo).release(Semver("1.0.0")) shouldBe Semver("1.0.0")
            }
            it("should be possible to release the version manually when no versions exist") {
                val tempDir = Files.createTempDirectory("semver-test")
                val git = Git.init().setGitDir(tempDir.toFile()).call()
                git.addCommit("Test commit")
                val props = Properties().apply { this["git.repo.directory"] = tempDir }
                val sv = SemverRelease(GitRepository(PropertiesConfiguration(props)))
                sv.release(Semver("1.0.0")) shouldBe Semver("1.0.0")
                tempDir.toFile().deleteRecursively()
            }
            it("should return null if version is less than latest version") {
                git().addRelease(0, Semver("2.0.0"))
                semverRelease(repo).release(Semver("1.0.0")) shouldBe null
            }
            it("should return null if version already exists") {
                semverRelease(repo).release(Semver("0.3.0")) shouldBe null
            }
        }

        describe("!mono-repo project") {
            it("should create next version if a module's sources have changed") {
                with(monorepoGit()) {
                    addCommit("Change foo", "foo/src/main")
                    addCommit("Release [patch]", "bar/src/main")
                }
                with(semverRelease(monoRepo)) {
                    nextIncrement() shouldBe Increment.PATCH
                    releaseModules(nextIncrement(), monoRepo.config.monorepo) shouldBe Semver("0.4.1") to listOf("foo", "bar")
                }
            }
/*
            it("should return latest version if a module's sources have NOT changed") {
                with(monorepoGit()) {
                    addCommit("Release [minor]", "bar/src/main")
                }
                with(semverRelease(monoRepo)) {
                    nextIncrement() shouldBe Increment.PATCH
                    releaseModules(nextIncrement()) shouldBe Semver("0.5.0") to listOf("bar")
                }
            }
*/
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
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
        monorepoTestConfig.git.repo.directory.toFile().deleteRecursively()
    }
}
