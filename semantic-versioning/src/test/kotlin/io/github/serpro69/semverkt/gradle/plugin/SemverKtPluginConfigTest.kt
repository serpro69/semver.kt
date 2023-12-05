package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.release.configuration.ModuleConfig
import io.github.serpro69.semverkt.release.configuration.CleanRule
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import kotlin.io.path.Path

class SemverKtPluginConfigTest : DescribeSpec({

    describe("Plugin Configuration") {
        context("configuration dsl functions") {
            val config = with(SemverKtPluginConfig(null)) {
                git {
                    tag {
                        prefix = "test"
                    }
                    message {
                        ignoreCase = true
                    }
                    repo {
                        remoteName = "not origin"
                    }
                }
                version {
                    initialVersion = Semver("1.2.3")
                    defaultIncrement = Increment.NONE
                }
                monorepo {
                    modules.add(ModuleConfig("foo"))
                    module("bar") {
                        sources = Path("src/main/kotlin")
                    }
                }
            }
            it("should override default configuration") {
                assertSoftly {
                    config.git.tag.prefix shouldBe "test"
                    config.git.message.ignoreCase shouldBe true
                    config.git.repo.remoteName shouldBe "not origin"
                    config.version.initialVersion shouldBe Semver("1.2.3")
                    config.version.defaultIncrement shouldBe Increment.NONE
                    config.monorepo.modules.size shouldBe 2
                    config.monorepo.modules.map { it.name } shouldBe listOf("foo", "bar")
                }
            }
            it("should have default configuration intact") {
                config.git.tag.separator shouldBe ""
                config.git.message.major shouldBe "[major]"
                config.git.repo.directory shouldBe Path(".")
                config.git.repo.cleanRule shouldBe CleanRule.TRACKED
                config.version.preReleaseId shouldBe "rc"
                config.version.placeholderVersion shouldBe Semver("0.0.0")
            }
            it("should throw exception for empty module name") {
                assertThrows<IllegalArgumentException> {
                    with(SemverKtPluginConfig(null)) {
                        monorepo {
                            module("") {}
                        }
                    }
                }
            }
        }
    }
})
