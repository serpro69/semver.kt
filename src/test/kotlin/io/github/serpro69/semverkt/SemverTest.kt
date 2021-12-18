package io.github.serpro69.semverkt

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

// Semantic Versioning Specification 2.0.0
class SemverTest : DescribeSpec({
    describe("2. A normal version number MUST take the form X.Y.Z where X, Y, and Z are non-negative integers") {
        context("valid version") {
            it("contains non-negative integers w/o leading zeroes") {
                shouldNotThrow<IllegalVersionException> { Semver("1.9.0") }
                shouldNotThrow<IllegalVersionException> { Semver(1, 9, 0) }
            }
        }
        context("invalid version") {
            it("contains leading zeroes") {
                shouldThrow<IllegalVersionException> { Semver("1.09.0") }
            }
            it("contains negative integers") {
                shouldThrow<IllegalVersionException> { Semver("-1.09.0") }
            }
        }
        // TODO
//        context("Each element MUST increase numerically. For instance: 1.9.0 -> 1.10.0 -> 1.11.0.") {
//
//        }
    }

    describe("4. Major version zero (0.y.z) is for initial development.") {
        context("valid version") {
            it("can start with '0' for major version number") {
                shouldNotThrow<IllegalVersionException> { Semver("0.1.0") }
                shouldNotThrow<IllegalVersionException> { Semver(0, 1, 0) }
            }
        }
    }
})
