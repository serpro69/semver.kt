package io.github.serpro69.semverkt.release.configuration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DefaultConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("DefaultConfiguration") {
        val dc = DefaultConfiguration
        val provider = object : ConfigurationProvider() {}

        it("should return default property values") {
            // git.repo
            dc.git.repo.directory shouldBe provider.git.repo.directory
            dc.git.repo.remoteName shouldBe provider.git.repo.remoteName
            dc.git.repo.cleanRule shouldBe provider.git.repo.cleanRule
            // git.tag
            dc.git.tag.prefix shouldBe provider.git.tag.prefix
            dc.git.tag.separator shouldBe provider.git.tag.separator
            dc.git.tag.useBranches shouldBe provider.git.tag.useBranches
            // git.message
            dc.git.message.major shouldBe provider.git.message.major
            dc.git.message.minor shouldBe provider.git.message.minor
            dc.git.message.patch shouldBe provider.git.message.patch
            dc.git.message.preRelease shouldBe provider.git.message.preRelease
            dc.git.message.ignoreCase shouldBe provider.git.message.ignoreCase
            // version
            dc.version.initialVersion shouldBe provider.version.initialVersion
            dc.version.placeholderVersion shouldBe provider.version.placeholderVersion
            dc.version.defaultIncrement shouldBe provider.version.defaultIncrement
            dc.version.preReleaseId shouldBe provider.version.preReleaseId
            dc.version.initialPreRelease shouldBe provider.version.initialPreRelease
            dc.version.snapshotSuffix shouldBe provider.version.snapshotSuffix
            // monorepo
            dc.monorepo.sources shouldBe provider.monorepo.sources
            dc.monorepo.modules shouldBe provider.monorepo.modules
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
