package io.github.serpro69.semverkt.release.configuration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.*

class PropertiesConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("PropertiesConfiguration") {
        val properties = Properties().also {
            it["git.tag.prefix"] = "foo"
            it["git.tag.separator"] = "bar"
        }
        val pc = PropertiesConfiguration(properties)

        it("should return overridden property values") {
            pc.git.tag.prefix shouldBe TagPrefix("foo")
            pc.git.tag.separator shouldBe "bar"
        }

        it("should return default property values") {
            pc.git.repo.remoteName shouldBe "origin"
            pc.git.repo.cleanRule shouldBe CleanRule.TRACKED
            pc.git.tag.useBranches shouldBe false
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
