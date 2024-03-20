package io.github.serpro69.semverkt.release.configuration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path

class DslConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("DslConfiguration") {
        context("'git' config is applied before 'monorepo'") {
            val c = DslConfiguration {
                git {
                    repo {
                        directory = Path("foo/bar")
                    }
                    tag {
                        prefix = TagPrefix("p")
                        separator = "sep"
                    }
                }
                monorepo {
                    sources = this@DslConfiguration.git.repo.directory
                    module("foo") {
                        sources = Path("src")
                        tag {
                            prefix = TagPrefix("foo-v")
                        }
                    }
                    module("bar") {}
                    module("baz") {}
                }
            }

            it("should return overridden property values") {
                c.git.repo.directory shouldBe Path("foo/bar")
                c.git.tag.prefix shouldBe TagPrefix("p")
                c.git.tag.separator shouldBe "sep"
                c.monorepo.sources shouldBe c.git.repo.directory
                c.monorepo.modules shouldHaveSize 3
                c.monorepo.modules.map { it.path } shouldContainExactly listOf("foo", "bar", "baz")
                with(c.monorepo.modules.first { it.path == "foo" }) {
                    // overwritten for module
                    tag?.prefix shouldBe TagPrefix("foo-v")
                    // overwritten for git config, so should be applied to module also
                    tag?.separator shouldBe "sep"
                    // use default
                    tag?.useBranches shouldBe false
                }
                with(c.monorepo.modules.first { it.path == "bar" }) { tag shouldBe null }
                with(c.monorepo.modules.first { it.path == "baz" }) { tag shouldBe null }
            }

            it("should return default property values") {
                c.git.repo.remoteName shouldBe "origin"
                c.git.repo.cleanRule shouldBe CleanRule.TRACKED
                c.git.tag.useBranches shouldBe false
            }
        }

        context("'git' config is applied after 'monorepo'") {
            val c = DslConfiguration {
                monorepo {
                    sources = Path("src/main")
                    module("foo") {
                        tag {
                            prefix = TagPrefix("foo-v")
                        }
                    }
                    module("bar") {}
                    module("baz") {}
                }
                git {
                    repo {
                    }
                    tag {
                        prefix = TagPrefix("p")
                        separator = "sep"
                    }
                }
            }

            it("should return overridden property values") {
                c.git.tag.prefix shouldBe TagPrefix("p")
                c.git.tag.separator shouldBe "sep"
                c.monorepo.sources shouldBe Path("src/main")
                c.monorepo.modules shouldHaveSize 3
                c.monorepo.modules.map { it.path } shouldContainExactly listOf("foo", "bar", "baz")
                with(c.monorepo.modules.first { it.path == "foo" }) {
                    // overwritten for module
                    tag?.prefix shouldBe TagPrefix("foo-v")
                    // git is applied after monorepo, so we have a "global default" here
                    tag?.separator shouldBe ""
                    // use default
                    tag?.useBranches shouldBe false
                }
                with(c.monorepo.modules.first { it.path == "bar" }) { tag shouldBe null }
                with(c.monorepo.modules.first { it.path == "baz" }) { tag shouldBe null }
            }

            it("should return default property values") {
                c.git.repo.remoteName shouldBe "origin"
                c.git.repo.cleanRule shouldBe CleanRule.TRACKED
                c.git.tag.useBranches shouldBe false
            }
        }

        it("should throw exception for empty module name") {
            assertThrows<IllegalArgumentException> {
                DslConfiguration {
                    monorepo {
                        module("") {}
                    }
                }
            }
        }
    }
})
