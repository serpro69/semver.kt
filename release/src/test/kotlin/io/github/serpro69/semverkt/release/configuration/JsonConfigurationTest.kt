package io.github.serpro69.semverkt.release.configuration

import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.io.path.Path

class JsonConfigurationTest : DescribeSpec({

    assertSoftly = true

    describe("JsonConfiguration") {
        val config: (json: String) -> JsonConfiguration = { JsonConfiguration(it) }

        it("should return overridden property values") {
            val json = """
            {
              "git": {
                "repo": {
                  "directory": "/tmp/foo/bar",
                  "remoteName": "baz",
                  "cleanRule": "all"
                },
                "tag": {
                  "prefix": "p",
                  "separator": "sep",
                  "useBranches": true,
                },
                "message": {
                  "major": "[maj]",
                  "minor": "[min]",
                  "patch": "[pat]",
                  "preRelease": "[pre]",
                  "ignoreCase": true
                }
              },
              "version": {
                "initialVersion": "10.0.0",
                "placeholderVersion": "0.0.0-dev",
                "defaultIncrement": "patch",
                "preReleaseId": "prerelease",
                "initialPreRelease": "10",
                "snapshotSuffix": "OHH-SNAP"
              },
              "monorepo": [
                {
                  "path": "foo",
                  "sources": "src",
                  "tag": {
                    "prefix": "foo-v",
                    "separator": "foo-sep"
                  }
                },
                {
                  "path": "bar",
                  "sources": "./bar",
                  "tag": {
                    "separator": "bar-sep",
                    "useBranches": true,
                  }
                },
                {
                  "path": "baz",
                  "sources": "./baz"
                }
              ]
            }
            """.trimIndent()
            val jc = config(json)
            // git.repo properties
            jc.git.repo.directory shouldBe Path("/tmp/foo/bar")
            jc.git.repo.remoteName shouldBe "baz"
            jc.git.repo.cleanRule shouldBe CleanRule.ALL

            // git.tag properties
            jc.git.tag.prefix shouldBe TagPrefix("p")
            jc.git.tag.separator shouldBe "sep"
            jc.git.tag.useBranches shouldBe true

            // git.message properties
            jc.git.message.major shouldBe "[maj]"
            jc.git.message.minor shouldBe "[min]"
            jc.git.message.patch shouldBe "[pat]"
            jc.git.message.preRelease shouldBe "[pre]"
            jc.git.message.ignoreCase shouldBe true

            // version properties
            jc.version.initialVersion shouldBe Semver("10.0.0")
            jc.version.placeholderVersion shouldBe Semver("0.0.0-dev")
            jc.version.defaultIncrement shouldBe Increment.PATCH
            jc.version.preReleaseId shouldBe "prerelease"
            jc.version.initialPreRelease shouldBe 10
            jc.version.snapshotSuffix shouldBe "OHH-SNAP"

            // monorepo properties
            jc.monorepo.modules shouldHaveSize 3
            // monorepo[0] properties
            jc.monorepo.modules[0].path shouldBe "foo"
            jc.monorepo.modules[0].sources shouldBe Path("src")
            jc.monorepo.modules[0].tag?.prefix shouldBe TagPrefix("foo-v")
            jc.monorepo.modules[0].tag?.separator shouldBe "foo-sep"
            jc.monorepo.modules[0].tag?.useBranches shouldBe false // uses "global default" since not overwritten for module
            // monorepo[1] properties
            jc.monorepo.modules[1].path shouldBe "bar"
            jc.monorepo.modules[1].sources shouldBe Path("./bar")
            jc.monorepo.modules[1].tag?.prefix shouldBe TagPrefix("v") // uses "global default" since not overwritten for module
            jc.monorepo.modules[1].tag?.separator shouldBe "bar-sep"
            jc.monorepo.modules[1].tag?.useBranches shouldBe true
            // monorepo[2] properties
            jc.monorepo.modules[2].path shouldBe "baz"
            jc.monorepo.modules[2].sources shouldBe Path("./baz")
            jc.monorepo.modules[2].tag shouldBe null
        }

        it("should return default property values") {
            val jc = config("{}")
            // git.repo
            jc.git.repo.directory shouldBe DefaultConfiguration.git.repo.directory
            jc.git.repo.remoteName shouldBe DefaultConfiguration.git.repo.remoteName
            jc.git.repo.cleanRule shouldBe DefaultConfiguration.git.repo.cleanRule
            // git.tag
            jc.git.tag.prefix shouldBe DefaultConfiguration.git.tag.prefix
            jc.git.tag.separator shouldBe DefaultConfiguration.git.tag.separator
            jc.git.tag.useBranches shouldBe DefaultConfiguration.git.tag.useBranches
            // git.message
            jc.git.message.major shouldBe DefaultConfiguration.git.message.major
            jc.git.message.minor shouldBe DefaultConfiguration.git.message.minor
            jc.git.message.patch shouldBe DefaultConfiguration.git.message.patch
            jc.git.message.preRelease shouldBe DefaultConfiguration.git.message.preRelease
            jc.git.message.ignoreCase shouldBe DefaultConfiguration.git.message.ignoreCase
            // version
            jc.version.initialVersion shouldBe DefaultConfiguration.version.initialVersion
            jc.version.placeholderVersion shouldBe DefaultConfiguration.version.placeholderVersion
            jc.version.defaultIncrement shouldBe DefaultConfiguration.version.defaultIncrement
            jc.version.preReleaseId shouldBe DefaultConfiguration.version.preReleaseId
            jc.version.initialPreRelease shouldBe DefaultConfiguration.version.initialPreRelease
            jc.version.snapshotSuffix shouldBe DefaultConfiguration.version.snapshotSuffix
            // monorepo
            jc.monorepo.modules shouldBe DefaultConfiguration.monorepo.modules
        }

        context("mandatory 'path' property for module configuration") {
            it("should throw an exception when 'path' is blank") {
                val json = """{ "monorepo": [ { "path": "" } ] }"""
                shouldThrow<IllegalArgumentException> {
                    // access monorepo property since it's lazy
                    config(json).monorepo.modules
                }
            }

            it("should throw an exception when 'path' is null") {
                val json = """{ "monorepo": [ { "path": null } ] }"""
                shouldThrow<IllegalArgumentException> {
                    // access monorepo property since it's lazy
                    config(json).monorepo.modules
                }
            }

            it("should throw an exception when 'path' is absent") {
                val json = """{ "monorepo": [ {  } ] }"""
                shouldThrow<IllegalArgumentException> {
                    // access monorepo property since it's lazy
                    config(json).monorepo.modules
                }
            }
        }
    }
})
