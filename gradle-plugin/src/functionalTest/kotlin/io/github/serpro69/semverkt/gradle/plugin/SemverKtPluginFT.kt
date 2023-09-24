package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.semverkt.gradle.plugin.fixture.SemverKtTestProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class SemverKtPluginFT : DescribeSpec({

    assertSoftly = true

    describe("versioning from commits") {
        listOf("major", "minor", "patch").forEach { keyword ->
            it("test initial version from commit message with [$keyword] keyword") {
                val project = SemverKtTestProject()
                Git.open(project.projectDir.toFile()).use {
                    project.projectDir.resolve("text.txt").createFile().writeText("Hello")
                    it.add().addFilepattern("text.txt").call()
                    it.commit().setMessage("New commit\n\n[$keyword]").call()
                }
                val result = Builder.build(project = project, args = arrayOf("tag", "-PdryRun"))
                result.task(":tag")?.outcome shouldBe TaskOutcome.SUCCESS
                // initial version should be calculated from config, so the keyword value doesn't really matter so long as it's valid
                result.output shouldContain "Calculated next version: 0.1.0"
            }
        }
    }
})
