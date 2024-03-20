package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.*
import kotlin.io.path.Path

class PropertiesConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("PropertiesConfiguration") {

        it("should return overridden property values") {
            val properties = Properties().also {
                // test overriding one of each config property types
                // Path
                it["git.repo.directory"] = "/tmp/foobar"
                it["monorepo.sources"] = "/tmp/foobar/baz"
                // String
                it["git.repo.remoteName"] = "remote"
                // CleanRule
                it["git.repo.cleanRule"] = CleanRule.NONE
                // TagPrefix
                it["git.tag.prefix"] = "foo-v"
                // Boolean
                it["git.tag.useBranches"] = true
                // Semver
                it["version.initialVersion"] = "1.2.3"
                // Increment
                it["version.defaultIncrement"] = Increment.PATCH
                // Int
                it["version.initialPreRelease"] = 10
            }
            val pc = PropertiesConfiguration(properties)
            pc.git.repo.directory shouldBe Path("/tmp/foobar")
            pc.git.repo.remoteName shouldBe "remote"
            pc.git.repo.cleanRule shouldBe CleanRule.NONE
            pc.git.tag.prefix shouldBe TagPrefix("foo-v")
            pc.git.tag.useBranches shouldBe true
            pc.version.initialVersion shouldBe Semver("1.2.3")
            pc.version.defaultIncrement shouldBe Increment.PATCH
            pc.version.initialPreRelease shouldBe 10
            pc.monorepo.sources shouldBe Path("/tmp/foobar/baz")
        }

        it("should return default property values") {
            val pc = PropertiesConfiguration(Properties())
            // git.repo
            pc.git.repo.directory shouldBe DefaultConfiguration.git.repo.directory
            pc.git.repo.remoteName shouldBe DefaultConfiguration.git.repo.remoteName
            pc.git.repo.cleanRule shouldBe DefaultConfiguration.git.repo.cleanRule
            // git.tag
            pc.git.tag.prefix shouldBe DefaultConfiguration.git.tag.prefix
            pc.git.tag.separator shouldBe DefaultConfiguration.git.tag.separator
            pc.git.tag.useBranches shouldBe DefaultConfiguration.git.tag.useBranches
            // git.message
            pc.git.message.major shouldBe DefaultConfiguration.git.message.major
            pc.git.message.minor shouldBe DefaultConfiguration.git.message.minor
            pc.git.message.patch shouldBe DefaultConfiguration.git.message.patch
            pc.git.message.preRelease shouldBe DefaultConfiguration.git.message.preRelease
            pc.git.message.ignoreCase  shouldBe DefaultConfiguration.git.message.ignoreCase
            // version
            pc.version.initialVersion shouldBe DefaultConfiguration.version.initialVersion
            pc.version.placeholderVersion shouldBe DefaultConfiguration.version.placeholderVersion
            pc.version.defaultIncrement shouldBe DefaultConfiguration.version.defaultIncrement
            pc.version.preReleaseId shouldBe DefaultConfiguration.version.preReleaseId
            pc.version.initialPreRelease shouldBe DefaultConfiguration.version.initialPreRelease
            pc.version.snapshotSuffix shouldBe DefaultConfiguration.version.snapshotSuffix
            // monorepo
            pc.monorepo.sources shouldBe DefaultConfiguration.monorepo.sources
            pc.monorepo.modules shouldBe DefaultConfiguration.monorepo.modules
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
