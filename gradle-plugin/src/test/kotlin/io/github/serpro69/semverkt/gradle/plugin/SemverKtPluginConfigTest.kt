package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.Path

class SemverKtPluginConfigTest : DescribeSpec({

    describe("Plugin Configuration") {
        context("configuration dsl functions") {
            val config = with(SemverKtPluginConfig()) {
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
            }
            it("should override default configuration") {
                assertSoftly {
                    config.git.tag.prefix shouldBe "test"
                    config.git.message.ignoreCase shouldBe true
                    config.git.repo.remoteName shouldBe "not origin"
                    config.version.initialVersion shouldBe Semver("1.2.3")
                    config.version.defaultIncrement shouldBe Increment.NONE
                }
            }
            it("should have default configuration intact") {
                config.git.tag.separator shouldBe ""
                config.git.message.major shouldBe "[major]"
                config.git.repo.directory shouldBe Path(".")
                config.version.preReleaseId shouldBe "rc"
            }
        }
    }
})
