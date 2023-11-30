package io.github.serpro69.semverkt.release.configuration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class JsonConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("JsonConfiguration") {
        val json = """
            {
                "git": {
                    "tag": {
                        "prefix": "foo",
                        "separator": "bar"
                    }
                }
            }
        """.trimIndent()
        val jc = JsonConfiguration(json)

        it("should return overridden property values") {
                jc.git.tag.prefix shouldBe "foo"
                jc.git.tag.separator shouldBe "bar"
        }

        it("should return default property values") {
                jc.git.repo.remoteName shouldBe "origin"
                jc.git.tag.useBranches shouldBe false
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
