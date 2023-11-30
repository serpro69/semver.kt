package io.github.serpro69.semverkt.release.configuration

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class DslConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("DslConfiguration") {
        val c = DslConfiguration {
            git {
                repo {
                }
                tag {
                    prefix = "p"
                    separator = "sep"
                }
            }
            monorepo {
                module {
                    name = "foo"
                }
                module {
                    name = "bar"
                }
                module {
                    name = "baz"
                }
            }
        }

        it("should return overridden property values") {
            c.git.tag.prefix shouldBe "p"
            c.git.tag.separator shouldBe "sep"
            c.monorepo.modules shouldHaveSize 3
            c.monorepo.modules.map { it.name } shouldContainExactly listOf("foo", "bar", "baz")
        }

        it("should return default property values") {
            c.git.repo.remoteName shouldBe "origin"
            c.git.tag.useBranches shouldBe false
        }
    }
})
