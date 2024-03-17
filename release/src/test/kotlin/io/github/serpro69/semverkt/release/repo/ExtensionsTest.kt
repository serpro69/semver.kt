package io.github.serpro69.semverkt.release.repo

import io.github.serpro69.semverkt.release.TestFixtures
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.matchers.shouldBe

class ExtensionsTest : TestFixtures({ test: TestFixtures ->

    describe("repo extension functions") {
        it("semver() fun") {
            semver(test.testConfiguration.git.tag.prefix)(test.repo.latestVersionTag()!!) shouldBe Semver("0.4.0")
        }
    }
})
