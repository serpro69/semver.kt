package io.github.serpro69.semverkt.release

import io.github.serpro69.semverkt.release.configuration.PropertiesConfiguration
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import java.util.*

class SemverReleaseTest : TestFixtures({ test ->

    listOf(
        Triple(test.git, test.repo, null),
        Triple(test.monoGit, test.monoRepo, "foo")
    ).forEach { (git, repo, submodule) ->
        val testName: (String) -> String = { it + if (submodule != null) " in monorepo" else "" }
        describe(testName("next version Increment from commit")) {
            it("should return MAJOR") {
                git().use {
                    it.addCommit("Release [major]")
                    it.addCommit("Release [pre release]")
                    it.addCommit("Release [minor]")
                    it.addCommit("Release [patch]")
                }
                test.semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.MAJOR
                test.semverRelease(repo).use { it.nextIncrement(submodule) } shouldBe Increment.MAJOR
            }
            it("should return MINOR") {
                git().use {
                    it.addCommit("Release [pre release]")
                    it.addCommit("Release [minor]")
                    it.addCommit("Release [patch]")
                }
                test.semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.MINOR
                test.semverRelease(repo).use { it.nextIncrement(submodule) } shouldBe Increment.MINOR
            }
            it("should return PATCH") {
                git().use {
                    it.addCommit("Release [patch]")
                    it.addCommit("Release [pre release]")
                }
                test.semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.PATCH
                test.semverRelease(repo).use { it.nextIncrement(submodule) } shouldBe Increment.PATCH
            }
            it("should return PRE_RELEASE") {
                git().use { it.addCommit("Release [pre release]") }
                test.semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.PRE_RELEASE
                test.semverRelease(repo).use { it.nextIncrement(submodule) } shouldBe Increment.PRE_RELEASE
            }
            context("should return DEFAULT") {
                it("when no keywords found") {
                    git().use { it.addCommit("Not a release") }
                    test.semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.DEFAULT
                    test.semverRelease(repo).use { it.nextIncrement(submodule) } shouldBe Increment.DEFAULT
                }
            }
            context("should return NONE") {
                it("when HEAD points to latest release") {
                    git().use { it.addRelease(0, Semver("1.0.0")) }
                    test.semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.NONE
                    if (submodule != null) {
                        test.monoGit().use { it.addRelease(0, Semver("1.0.0"), submodule = "foo") }
                        test.semverRelease(test.monoRepo).use { it.nextIncrement(submodule) } shouldBe Increment.NONE
                    }
                }
                it("when HEAD points to a release tag") {
                    git().use {
                        it.addRelease(3, Semver("1.0.0"))
                        it.checkout().setName("v0.4.0").call()
                    }
                    test.semverRelease(repo).use { it.nextIncrement() } shouldBe Increment.NONE
                    if (submodule != null) {
                        test.monoGit().use { it.checkout().setName("foo-v0.2.0").call() }
                        test.semverRelease(test.monoRepo).use { it.nextIncrement(submodule) } shouldBe Increment.NONE
                    }
                }
            }
        }

        describe(testName("Semver")) {
            listOf(
                Semver("1.0.0-SNAPSHOT"),
                Semver("1.0.0-rc.1"),
                Semver("1.0.0")
            ).forEach { v ->
                it("increment major release after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                        it.addRelease(0, v, submodule = submodule)
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use { it.release(Increment.MAJOR) } shouldBe Semver("2.0.0")
                    test.semverRelease(repo).use { it.release(Increment.MAJOR, submodule) } shouldBe Semver("2.0.0")
                }

                it("increment major snapshot after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                        it.addRelease(0, v, submodule = submodule)
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use { it.snapshot(Increment.MAJOR) } shouldBe Semver("2.0.0-SNAPSHOT")
                    test.semverRelease(repo).use { it.snapshot(Increment.MAJOR, submodule) } shouldBe Semver("2.0.0-SNAPSHOT")
                }

                it("increment minor release after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                        it.addRelease(0, v, submodule = submodule)
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use { it.release(Increment.MINOR) } shouldBe Semver("1.1.0")
                    test.semverRelease(repo).use { it.release(Increment.MINOR, submodule) } shouldBe Semver("1.1.0")
                }

                it("increment minor snapshot after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                        it.addRelease(0, v, submodule = submodule)
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use { it.snapshot(Increment.MINOR) } shouldBe Semver("1.1.0-SNAPSHOT")
                    test.semverRelease(repo).use { it.snapshot(Increment.MINOR, submodule) } shouldBe Semver("1.1.0-SNAPSHOT")
                }

                it("increment patch release after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                        it.addRelease(0, v, submodule = submodule)
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use { it.release(Increment.PATCH) } shouldBe Semver("1.0.1")
                    test.semverRelease(repo).use { it.release(Increment.PATCH, submodule) } shouldBe Semver("1.0.1")
                }

                it("increment patch snapshot after $v") {
                    git().use {
                        it.addRelease(0, v)
                        it.addCommit("Test commit")
                        it.addRelease(0, v, submodule = submodule)
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use { it.snapshot(Increment.PATCH) } shouldBe Semver("1.0.1-SNAPSHOT")
                    test.semverRelease(repo).use { it.snapshot(Increment.PATCH, submodule) } shouldBe Semver("1.0.1-SNAPSHOT")
                }
            }

            it("increment pre_release") {
                git().use { git ->
                    test.semverRelease(repo).use {
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
                    test.semverRelease(repo).use {
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
                    test.semverRelease(repo).use {
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
                    test.semverRelease(repo).use {
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
                    test.semverRelease(repo).use {
                        it.createPreRelease(Increment.MAJOR) shouldBe Semver("1.0.0-SNAPSHOT")
                        it.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-SNAPSHOT")
                        it.createPreRelease(Increment.DEFAULT) shouldBe Semver("1.0.0-SNAPSHOT")
                    }
                }

                it("pre-release version should not change") {
                    git().use { it.addRelease(1, Semver("1.0.0-rc.3")) }
                    test.semverRelease(repo).use {
                        it.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0-rc.3")
                        it.createPreRelease(Increment.NONE) shouldBe Semver("1.0.0-rc.3")
                    }
                }

                it("release version should not change") {
                    git().use {
                        it.addRelease(0, Semver("1.0.0"))
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use {
                        it.createPreRelease(Increment.PRE_RELEASE) shouldBe Semver("1.0.0")
                        it.createPreRelease(Increment.NONE) shouldBe Semver("1.0.0")
                    }
                }

                it("first pre-release version should be created for the specified increment") {
                    git().use {
                        it.addRelease(0, Semver("1.0.0"))
                        it.addCommit("Test commit")
                    }
                    test.semverRelease(repo).use {
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
                    test.semverRelease(repo).use {
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
                    test.semverRelease(repo).use { it.promoteToRelease() } shouldBe Semver("1.0.0")
                }

                it("should return current version if not on pre-release") {
                    git().use { it.addRelease(0, Semver("1.0.0")) }
                    test.semverRelease(repo).use {
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

        it(testName("should not take invalid tag for consideration")) {
            git().use {
                it.addRelease(0, Semver("1.0.0"))
                it.addCommit("Test commit")
                it.tag().setAnnotated(true).setName("x1.2.3").setForceUpdate(true).call()
                it.addCommit("Test commit")
            }
            test.semverRelease(repo).use { it.release(Increment.MAJOR) } shouldBe Semver("2.0.0")
        }

        describe(testName("manual release")) {
            it("should be possible to release the version manually when versions exist") {
                test.semverRelease(repo).use { it.release(Semver("1.0.0")) } shouldBe Semver("1.0.0")
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
                test.semverRelease(repo).use { it.release(Semver("1.0.0")) } shouldBe null
            }

            it("should return null if version already exists") {
                test.semverRelease(repo).use { it.release(Semver("0.3.0")) } shouldBe null
            }
        }
    }
})
