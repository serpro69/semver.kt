package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.kotest.core.spec.style.DescribeSpec

class SemverKtPluginFT : DescribeSpec({

    describe("gradle-semver-release plugin") {
        it("test me") {
            val result = Builder.build(project = SemverKtTestProject(), args = arrayOf("ft"))

            println(result.output)
        }
    }
})
