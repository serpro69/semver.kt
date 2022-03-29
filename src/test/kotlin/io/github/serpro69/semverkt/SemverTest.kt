package io.github.serpro69.semverkt

import io.github.serpro69.kfaker.faker
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.beEqualComparingTo
import io.kotest.matchers.comparables.beGreaterThan
import io.kotest.matchers.comparables.beLessThan
import io.kotest.matchers.shouldBe

// Semantic Versioning Specification 2.0.0
class SemverTest : DescribeSpec({
    val faker = faker { }
    describe("2. A normal version number MUST take the form X.Y.Z where X, Y, and Z are non-negative integers") {
        context("valid version") {
            it("contains non-negative integers w/o leading zeroes") {
                shouldNotThrow<IllegalVersionException> { Semver("1.9.0") }
                shouldNotThrow<IllegalVersionException> { Semver(1, 9, 0) }
            }
            it("should return normal part of the semver if it contains pre-release or build metadata") {
                assertSoftly {
                    Semver("1.2.3-rc.1").normalVersion shouldBe Semver("1.2.3")
                    Semver("1.2.3+build.369").normalVersion shouldBe Semver("1.2.3")
                    Semver("1.2.3-rc.1+build.369").normalVersion shouldBe Semver("1.2.3")
                }
            }
            it("should return pre-release") {
                Semver("1.2.3-rc.1").preRelease shouldBe PreRelease("rc.1")
            }
            it("pre-release should be null if absent") {
                Semver("1.2.3").preRelease shouldBe null
            }
            it("should return build metadata") {
                Semver("1.2.3+build.369").buildMetadata shouldBe BuildMetadata("build.369")
            }
            it("build metadata should be null if absent") {
                Semver("1.2.3").buildMetadata shouldBe null
            }
        }
        context("invalid version") {
            it("contains leading zeroes in version elements") {
                shouldThrow<IllegalVersionException> { Semver("1.09.0") }
            }
            it("contains negative integers in version elements") {
                shouldThrow<IllegalVersionException> { Semver("-1.09.0") }
            }
            it("is missing version elements") {
                shouldThrow<IllegalVersionException> { Semver("1.0") }
            }
        }
        context("Each element MUST increase numerically") {
            it("patch increase: 1.9.1 -> 1.9.2") {

            }
            it("minor increase: 1.9.0 -> 1.10.0") {

            }
            it("major increase: 1.9.0 -> 2.0.0") {

            }
        }
    }

    describe("4. Major version zero (0.y.z) is for initial development.") {
        context("valid version") {
            it("should be '0' for major version number") {
                shouldNotThrow<IllegalVersionException> { Semver("0.1.0") }
                shouldNotThrow<IllegalVersionException> { Semver(0, 1, 0) }
            }
        }
    }

    it("should increment patch version") {
        val semver = Semver("1.2.3").incrementPatch()
        assertSoftly {
            semver.major shouldBe 1
            semver.minor shouldBe 2
            semver.patch shouldBe 4
        }
    }

    describe("7. Patch version MUST be reset to 0 when minor version is incremented.") {
        val semver = Semver("1.2.3").incrementMinor()
        assertSoftly {
            semver.major shouldBe 1
            semver.minor shouldBe 3
            semver.patch shouldBe 0
        }
    }

    describe("8. Patch and minor versions MUST be reset to 0 when major version is incremented.") {
        val semver = Semver("1.2.3").incrementMajor()
        assertSoftly {
            semver.major shouldBe 2
            semver.minor shouldBe 0
            semver.patch shouldBe 0
        }
    }

    describe("9. A pre-release version MAY be denoted by appending a hyphen and a series of dot separated identifiers immediately following the patch version.") {
        //Examples: 1.0.0-alpha, 1.0.0-alpha.1, 1.0.0-0.3.7, 1.0.0-x.7.z.92, 1.0.0-x-y-z.–

        it("Identifiers MUST comprise only ASCII alphanumerics and hyphens [0-9A-Za-z-]") {
            assertSoftly {
                shouldNotThrow<IllegalVersionException> { Semver("1.0.0-alpha") }
                shouldNotThrow<IllegalVersionException> { Semver("1.0.0-alpha.1") }
                shouldNotThrow<IllegalVersionException> { Semver("1.0.0-0.3.7") }
                shouldNotThrow<IllegalVersionException> { Semver("1.0.0-x.7.z.92") }
                shouldNotThrow<IllegalVersionException> { Semver("1.0.0-x-y-z.–") }
            }
        }
        it("Identifiers MUST NOT be empty") {

        }
        it("Numeric identifiers MUST NOT include leading zeroes") {

        }
        it("Pre-release versions have a lower precedence than the associated normal version") {

        }
    }

    describe("10. Build metadata MAY be denoted by appending a plus sign and a series of dot separated identifiers immediately following the patch or pre-release version.") {
        //Examples: 1.0.0-alpha+001, 1.0.0+20130313144700, 1.0.0-beta+exp.sha.5114f85, 1.0.0+21AF26D3—-117B344092BD

        it("Identifiers MUST comprise only ASCII alphanumerics and hyphens [0-9A-Za-z-]") {
            assertSoftly {
                shouldNotThrow<IllegalVersionException> { Semver("1.2.3+${faker.random.nextInt()}") }
                shouldNotThrow<IllegalVersionException> { Semver("1.2.3+${faker.random.randomString()}-") }
                shouldNotThrow<IllegalVersionException> { Semver("1.2.3+${faker.random.nextInt()}.${faker.random.randomString()}") }
            }
        }
        it("Identifiers MUST NOT be empty") {
            shouldThrow<IllegalVersionException> { Semver("1.2.3+build..something") }
        }
        context("Build metadata MUST be ignored when determining version precedence") {
            it("Thus two versions that differ only in the build metadata, have the same precedence") {
                Semver("1.0.0+${faker.random.nextInt()}") shouldBe beEqualComparingTo(Semver("1.0.0+${faker.random.randomString()}"))
            }
        }
    }

    describe("11. Precedence refers to how versions are compared to each other when ordered.") {
        context("Precedence MUST be calculated by separating the version into major, minor, patch and pre-release identifiers in that order") {
            context("Precedence is determined by the first difference when comparing each of these identifiers from left to right as follows: Major, minor, and patch versions are always compared numerically.") {
                //Example: 1.0.0 < 2.0.0 < 2.1.0 < 2.1.1
                it("version with higher 'major' identifier takes precedence") {
                    Semver("1.0.0") shouldBe beLessThan(Semver("2.0.0"))
                }
                it("version with same 'major' and higher 'minor' identifier takes precedence") {
                    Semver("2.0.0") shouldBe beLessThan(Semver("2.1.0"))
                }
                it("version with same 'major', 'minor' and higher 'patch' identifier takes precedence") {
                    Semver("2.1.0") shouldBe beLessThan(Semver("2.1.1"))
                }
            }

            it("When major, minor, and patch are equal, a pre-release version has lower precedence than a normal version") {
                //Example: 1.0.0-alpha < 1.0.0
                Semver("1.0.0-alpha") shouldBe beLessThan(Semver("1.0.0"))
            }

            context("Precedence for two pre-release versions with the same major, minor, and patch version MUST be determined by comparing each dot separated identifier from left to right until a difference is found as follows") {
                //Example: 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0
                it("Identifiers consisting of only digits are compared numerically.") {
                    Semver("1.0.0-1") shouldBe beGreaterThan(Semver("1.0.0-0"))
                }
                it("Identifiers with letters or hyphens are compared lexically in ASCII sort order.") {
                    Semver("1.0.0-alpha") shouldBe beLessThan(Semver("1.0.0-alpha.1"))
                    Semver("1.0.0-alpha.beta") shouldBe beLessThan(Semver("1.0.0-beta"))
                    Semver("1.0.0-beta.2") shouldBe beLessThan(Semver("1.0.0-beta.11"))
                    Semver("1.0.0-beta.11") shouldBe beLessThan(Semver("1.0.0-rc.1"))
                }
                it("Numeric identifiers always have lower precedence than non-numeric identifiers.") {
                    Semver("1.0.0-alpha.1") shouldBe beLessThan(Semver("1.0.0-alpha.beta"))
                }
                it("A larger set of pre-release fields has a higher precedence than a smaller set, if all of the preceding identifiers are equal.") {
                    Semver("1.0.0-beta") shouldBe beLessThan(Semver("1.0.0-beta.2"))
                }
            }
        }

        it("Build metadata does not figure into precedence") {
            Semver("1.0.0+123") shouldBe beEqualComparingTo(Semver("1.0.0+456"))
        }
    }

})
