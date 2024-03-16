package io.github.serpro69.semverkt.release

import io.github.serpro69.kfaker.faker
import io.github.serpro69.semverkt.release.configuration.DslConfiguration
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import io.github.serpro69.semverkt.release.configuration.PropertiesConfiguration
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

private val faker = faker { }

private val testProperties = Properties().also {
    it["git.repo.directory"] = "build/test/resources/test-repo"
}

val testConfiguration = PropertiesConfiguration(testProperties)

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

val testRepo: () -> Git = {
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

val testMonoRepo: () -> Git = {
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
