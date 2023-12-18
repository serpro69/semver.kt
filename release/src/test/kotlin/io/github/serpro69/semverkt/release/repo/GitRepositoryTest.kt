package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.configuration.PropertiesConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.errors.RepositoryNotFoundException
import java.io.IOException
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class GitRepositoryTest : DescribeSpec({

    assertSoftly = true

    describe("git repository") {
        it("should throw exception of directory is not a git repo") {
            val dir = Path("build/test/resources/does-not-exist")
            val testProperties = Properties().also {
                it["git.repo.directory"] = dir
            }
            val testConfiguration = PropertiesConfiguration(testProperties)
            val ex = shouldThrow<IOException> { GitRepository(testConfiguration).use { it.tags() } }
            ex.message shouldBe "Can't open $dir as git repository"
            ex.cause shouldBe RepositoryNotFoundException(dir.absolutePathString())
        }
    }
})
