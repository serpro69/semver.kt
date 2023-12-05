package io.github.serpro69.semverkt.release.configuration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DefaultConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("DefaultConfiguration") {
        val dc = DefaultConfiguration()

        it("should return default property values") {
            dc.git.repo.remoteName shouldBe "origin"
            dc.git.repo.cleanRule shouldBe CleanRule.TRACKED
            dc.git.tag.useBranches shouldBe false
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
