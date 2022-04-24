package io.github.serpro69.semverkt.release.configuration

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DefaultConfigurationTest : DescribeSpec({

    describe("DefaultConfiguration") {
        val dc = DefaultConfiguration()

        it("should return default property values") {
            assertSoftly {
                dc.git.repo.remoteName shouldBe "origin"
                dc.git.tag.useBranches shouldBe false
            }
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
