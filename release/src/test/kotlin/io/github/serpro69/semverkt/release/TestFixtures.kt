package io.github.serpro69.semverkt.release

import io.github.serpro69.kfaker.faker
import io.github.serpro69.semverkt.release.configuration.DslConfiguration
import io.github.serpro69.semverkt.release.configuration.PropertiesConfiguration
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.release.repo.GitRepository
import io.github.serpro69.semverkt.release.repo.Repository
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

abstract class TestFixtures : DescribeSpec {

    private constructor() : super()

    constructor(body: DescribeSpec.(TestFixtures) -> Unit) : super({ body(this, object : TestFixtures() {}) })

    init {
        assertSoftly = true
    }

    private val testProperties = Properties().also {
        it["git.repo.directory"] = "build/test/resources/test-repo"
    }

    private val createTestRepo: () -> Git = {
        with(Git.init()) {
            setDirectory(testConfiguration.git.repo.directory.createDirectories().toFile())
            call().also {
                it.addCommit("Initial Commit")
                it.addRelease(3, Semver("0.1.0"), true)
                it.addRelease(0, Semver("0.2.0"), true)
                it.addRelease(3, Semver("0.3.0"), false)
                it.addRelease(3, Semver("0.4.0"), true)
                it.addCommit("Commit #5")
                it.addCommit("Commit #6")
            }
        }
    }
    private val createTestMonoRepo: () -> Git = {
        with(Git.init()) {
            val rootDir = monorepoTestConfig.git.repo.directory
            setDirectory(rootDir.createDirectories().toFile())
            val git = call()
            monorepoTestConfig.monorepo.modules.forEach {
                val src = rootDir.resolve(it.path).resolve(it.sources).createDirectories()
                src.resolve("Main.kt").toFile().createNewFile()
            }
            git.also {
                it.addCommit("Initial Commit")
                // add regular version
                it.addRelease(3, Semver("0.1.0"), true)
                // add foo-v version for submodule
                it.addRelease(0, Semver("0.1.0"), true, submodule = "foo")
                // add regular version
                it.addRelease(0, Semver("0.2.0"), true)
                it.addRelease(3, Semver("0.3.0"), false)
                // add foo-v version for submodule
                it.addRelease(1, Semver("0.2.0"), false, submodule = "foo")
                // add regular version
                it.addRelease(3, Semver("0.4.0"), true)
                it.addCommit("Commit #5")
                it.addCommit("Commit #6")
            }
        }
    }

    /**
     * [PropertiesConfiguration] instance that sets a custom 'git.repo.directory' for tests
     */
    val testConfiguration = PropertiesConfiguration(testProperties)

    /**
     * [DslConfiguration] instance that sets a custom 'git.repo.directory'
     * and sets up 'monorepo' configuration for tests
     */
    val monorepoTestConfig = DslConfiguration {
        git {
            repo {
                directory = Path("build/test/resources/test-mono-repo")
            }
        }

        monorepo {
            // have one module with custom prefix and another with default one to test various scenarios
            module("foo") {
                tag {
                    prefix = TagPrefix("foo-v")
                }
            }
            module("bar") {}
        }
    }

    /**
     * Creates an instance of [Git] with this [testConfiguration]
     *
     * The client needs to make sure to close the [Git] repository resources by calling [Git.close]
     */
    val git: () -> Git = { Git.open(testConfiguration.git.repo.directory.toFile()) }

    /**
     * Provides access to an instance of [Repository] with this [testConfiguration]
     *
     * This base class handles the instance initialization and closure via [beforeTest] and [afterTest] overrides.
     */
    var repo: Repository = GitRepository(testConfiguration)
        private set

    /**
     * Creates an instance of [Git] for monorepo with this [monorepoTestConfig]
     *
     * The client needs to make sure to close the [Git] repository resources by calling [Git.close]
     */
    val monoGit: () -> Git = { Git.open(monorepoTestConfig.git.repo.directory.toFile()) }

    /**
     * Provides access to an instance of mono-[Repository] with this [monorepoTestConfig]
     *
     * This base class handles the instance initialization and closure via [beforeTest] and [afterTest] overrides.
     */
    var monoRepo: Repository = GitRepository(monorepoTestConfig)
        private set

    /**
     * Returns an instance of [SemverRelease] for a given [Repository] input.
     *
     * The client needs to make sure to close the [SemverRelease] resources by calling [SemverRelease.close]
     */
    val semverRelease: (r: Repository) -> SemverRelease = { SemverRelease(it) }

    override suspend fun beforeSpec(spec: Spec) {
        testConfiguration.git.repo.directory.toFile().deleteRecursively()
        monorepoTestConfig.git.repo.directory.toFile().deleteRecursively()
    }

    override suspend fun beforeTest(testCase: TestCase) {
        repo = GitRepository(testConfiguration)
        monoRepo = GitRepository(monorepoTestConfig)
        createTestRepo()
        createTestMonoRepo()
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

private val faker = faker { }

fun Git.addRelease(
    noOfCommits: Int,
    version: Semver,
    annotated: Boolean = true,
    submodule: String? = null,
) {
    for (i in 0 until noOfCommits) {
        addCommit("Commit ${faker.random.randomString(10)}")
    }
    addCommit("Next release commit\n\nRelease version $version")
    val tag = submodule?.let { "$it-v$version" } ?: "v$version"
    tag().setAnnotated(annotated).setName(tag).setForceUpdate(true).call()
}

fun Git.addCommit(message: String, path: String = "", fileName: String = faker.random.randomString(10)): RevCommit {
    val repoPath = repository.directory.parentFile
    repoPath.resolve(path).resolve("$fileName.txt").createNewFile()
    add().addFilepattern(".").call()
    return commit().setMessage(message).call()
}
