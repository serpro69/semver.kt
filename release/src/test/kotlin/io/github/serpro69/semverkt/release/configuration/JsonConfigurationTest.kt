package io.github.serpro69.semverkt.release.configuration

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.*

class JsonConfigurationTest : DescribeSpec({

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
            assertSoftly {
                jc.git.tag.prefix shouldBe "foo"
                jc.git.tag.separator shouldBe "bar"
            }
        }

        it("should return default property values") {
            assertSoftly {
                jc.git.repo.remoteName shouldBe "origin"
                jc.git.tag.useBranches shouldBe false
            }
        }

        it("should throw an exception if mandatory property is not provided") {
            //TODO
//            shouldThrow<AutoKonfigException> {  }
        }
    }
})
