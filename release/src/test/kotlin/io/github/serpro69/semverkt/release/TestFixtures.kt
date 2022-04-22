package io.github.serpro69.semverkt.release

import io.github.serpro69.kfaker.faker
import io.github.serpro69.semverkt.release.configuration.PropertiesConfiguration
import io.github.serpro69.semverkt.spec.Semver
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

private val faker = faker { }

private val testProperties = Properties().also {
    it["git.repo.directory"] = Path("build/test/resources/test-repo")
}

val testConfiguration = PropertiesConfiguration(testProperties)

val testRepo: () -> Git = {
    val path = testConfiguration.git.repo.directory.createDirectories()
    with(Git.init()) {
        setDirectory(path.toFile())
        call().also {
            it.addCommit(path, "Initial Commit")
            it.addRelease(path, 0, Semver("0.2.0"), true)
            it.addRelease(path, 3, Semver("0.3.0"), false)
            it.addRelease(path, 3, Semver("0.4.0"), true)
            it.addCommit(path, "Commit #5")
            it.addCommit(path, "Commit #6")
        }
    }
}

private fun Git.addRelease(path: Path, noOfCommits: Int, version: Semver, annotated: Boolean) {
    for (i in 0 until noOfCommits) {
        addCommit(path, "Commit ${faker.random.randomString(10)}")
    }
    addCommit(path, "Next release commit\n\nRelease version $version")
    tag().setAnnotated(annotated).setName("v$version").setForceUpdate(true).call()
}

private fun Git.addCommit(repoPath: Path, message: String): RevCommit {
    repoPath.resolve("${faker.random.randomString(10)}.txt").toFile().createNewFile()
    add().addFilepattern(".").call()
    return commit().setMessage(message).call()
}

