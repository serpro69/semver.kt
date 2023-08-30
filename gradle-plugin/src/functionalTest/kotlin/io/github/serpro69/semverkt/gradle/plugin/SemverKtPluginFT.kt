package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

class SemverKtPluginFT : DescribeSpec({

    describe("gradle plugin semver-release") {
        it("test me") {
            val result = Builder.build(project = SemverKtTestProject(), args = arrayOf("ft"))
            result.task(":ft")?.outcome shouldBe TaskOutcome.NO_SOURCE
            result.output shouldContain "Project version: 1.2.3"
        }
    }
})
