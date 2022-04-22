package io.github.serpro69.semverkt.release.configuration

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.*

class PropertiesConfigurationTest : DescribeSpec({

    describe("PropertiesConfiguration") {
        val properties = Properties().also {
            it["git.tag.prefix"] = "foo"
            it["git.tag.separator"] = "bar"
        }
        val pc = PropertiesConfiguration(properties)

        it("should return overridden property values") {
            assertSoftly {
                pc.git.tag.prefix shouldBe "foo"
                pc.git.tag.separator shouldBe "bar"
            }
        }

        it("should return default property values") {
            assertSoftly {
                pc.git.repo.remoteName shouldBe "origin"
                pc.git.tag.useBranches shouldBe false
            }
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
