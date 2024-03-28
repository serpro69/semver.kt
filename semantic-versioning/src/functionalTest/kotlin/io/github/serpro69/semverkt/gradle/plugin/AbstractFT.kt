package io.github.serpro69.semverkt.gradle.plugin

import io.github.serpro69.kfaker.faker
import io.github.serpro69.semverkt.gradle.plugin.fixture.AbstractProject
import io.github.serpro69.semverkt.gradle.plugin.util.DryRun
import io.github.serpro69.semverkt.gradle.plugin.util.ReleaseFromCommit
import io.github.serpro69.semverkt.gradle.plugin.util.TestFixture
import io.github.serpro69.semverkt.gradle.plugin.util.TestFixtureCompanion
import io.github.serpro69.semverkt.gradle.plugin.util.UpToDate
import io.github.serpro69.semverkt.release.Increment
import io.github.serpro69.semverkt.spec.Semver
import io.kotest.common.ExperimentalKotest
import io.kotest.core.names.TestName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.core.spec.style.scopes.addContainer
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.reflect.full.companionObjectInstance

@OptIn(ExperimentalKotest::class)
abstract class AbstractFT : DescribeSpec {

    private constructor() : super()

    constructor(body: DescribeSpec.(AbstractFT) -> Unit) : super({ body(this, object : AbstractFT() {}) })

    init {
        assertSoftly = true
        concurrency = 10
    }

    /**
     * Expected :tag task output for a project/module with [name]
     */
    val expectedTag: (name: String, ver: Semver?, dryRun: DryRun, upToDate: UpToDate, tagExists: Boolean) -> String = { name, version, dryRun, upToDate, tagExists ->
        val noop = "Not doing anything"
        val sb = StringBuilder()
        sb.append("> Task :${if (name == "test-project") "" else "$name:"}tag")
        // when (dryRun) -> UP-TO-DATE will always be true
        upToDate(sb)(dryRun.value || upToDate.value)
        version?.let {
            sb.appendLine("Calculated next version: $it")
            if (!dryRun && tagExists) sb.appendLine("Tag v$it already exists in project")
        } ?: sb.appendLine(noop)
        sb.toString()
    }

    /**
     * Expected configure project output for a project [ver] with an optional [tag] and a list of [moduleVersions]
     */
    val expectedConfigure: (ver: Semver, modulesVersions: List<Pair<String, Semver>>) -> String = { version, modules ->
        val sb = StringBuilder()
        sb.appendLine("> Configure project :")
        sb.appendLine("Project test-project version: $version")
        modules.sortedBy { (m, _) -> m }.forEach { (m, v) ->
            sb.appendLine()
            sb.appendLine("> Configure project :$m")
            sb.appendLine("Module $m version: $v")
        }
        sb.toString()
    }

    val expectedOutcome: (dryRun: DryRun, upToDate: UpToDate) -> TaskOutcome = { dryRun, upToDate ->
        if (!dryRun && !upToDate) TaskOutcome.SUCCESS else TaskOutcome.UP_TO_DATE
    }

    val setupInitialVersions: (proj: AbstractProject, git: Git) -> (modules: List<Pair<String, Semver>>) -> Unit = { proj, git ->
        { modules ->
            proj.projectDir.resolve("text.txt").createFile().writeText(faker.lorem.words())
            git.add().addFilepattern(".").call()
            git.commit().setMessage("First commit").call()

            modules.shuffled().forEach { (m, v) ->
                val tagName = if (m == "core") "v$v" else "$m-v$v"
                // commit in 'root' together with 'core'
                if (m == "core") commit(proj, git, null)(null)
                commit(proj, git, null)(m)
                git.tag().setName(tagName).call()
            }
        }
    }

    /**
     * Commit to a given [module] (or in the root of the project if module is `null`) in a [project].
     */
    val commit: (project: AbstractProject, git: Git, inc: Increment?) -> (module: String?) -> Unit = { proj, git, inc ->
        { m ->
            if (m != null) {
                proj.projectDir.resolve(m)
                    .resolve(if (m == "core") "src" else "${m}src").createDirectories()
                    .resolve("${faker.random.nextUUID()}.txt")
                    .createFile()
                    .writeText(faker.lorem.words())
                git.add().addFilepattern(".").call()
                val msg = inc?.let {
                    val increment = if (it == Increment.PRE_RELEASE) "pre release" else it.toString()
                    "Commit in $m\n\n[$increment]"
                } ?: "Commit in $m$"
                git.commit().setMessage(msg).call()
            } else {
                proj.projectDir.resolve("${faker.random.nextUUID()}.txt")
                    .createFile()
                    .writeText(faker.lorem.words())
                git.add().addFilepattern(".").call()
                val msg = inc?.let {
                    val increment = if (it == Increment.PRE_RELEASE) "pre release" else it.toString()
                    "Commit in root\n\n[$increment]"
                } ?: "Commit in root"
                git.commit().setMessage(msg).call()
            }
        }
    }

    val upToDate: (StringBuilder) -> (Boolean) -> StringBuilder = { sb ->
        { upToDate -> if (upToDate) sb.appendLine(" UP-TO-DATE") else sb.appendLine() }
    }

}

fun DescribeSpec.describeWithDryRun(
    name: String,
    test: suspend DescribeSpecContainerScope.(DryRun) -> Unit
) {
    DryRun.forEach { dryRun ->
        describe(name.testName(dryRun = dryRun)) { test(this@describe, dryRun) }
//        addContainer(
//            TestName("Describe: ", name.testName(dryRun = dryRun), false),
//            disabled = false,
//            null
//        ) { DescribeSpecContainerScope(this@addContainer).test(dryRun) }
    }
}

fun DescribeSpec.describeWithRfc(
    name: String,
    test: DescribeSpecContainerScope.(ReleaseFromCommit) -> Unit
) {
    ReleaseFromCommit.forEach { rfc ->
        describe(name.testName(rfc = rfc)) { test(this@describe, rfc) }
    }
}

@Suppress("UNCHECKED_CAST")
internal inline fun <reified F: TestFixture> DescribeSpec.describeWith(
    name: String,
    crossinline test: DescribeSpecContainerScope.(F) -> Unit
) {
    val companion = F::class.companionObjectInstance as TestFixtureCompanion<F>
    companion.forEach {
        describe(name.testName(rfc = it as? ReleaseFromCommit, it as? DryRun)) { test(this@describe, it) }
    }
}

fun DescribeSpec.describeWithAll(
    name: String,
    test: suspend DescribeSpecContainerScope.(DryRun, ReleaseFromCommit) -> Unit
) {
    ReleaseFromCommit.forEach { rfc ->
        DryRun.forEach { dryRun ->
            describe(name.testName(rfc = rfc, dryRun = dryRun)) { test(this@describe, dryRun, rfc) }
//            addContainer(
//                TestName("Describe: ", name.testName(rfc = rfc, dryRun = dryRun), false),
//                disabled = false,
//                null
//            ) { DescribeSpecContainerScope(this@addContainer).test(dryRun, rfc) }
        }
    }
}

/**
 * Appends string to the test name with optional information for [rfc] and [dryRun]
 */
private fun String.testName(rfc: ReleaseFromCommit? = null, dryRun: DryRun? = null): String {
    val s = StringBuilder(this)
    rfc?.let { if (it.value) s.append(" via commit") else s.append(" via gradle property") }
    dryRun?.let { if (it.value) s.append(" and dryRun") }
    return s.toString()
}

val faker = faker {  }

val Git.commitMinor: (release: ReleaseFromCommit) -> Unit get() = {
    if (it.value) commit().setMessage("New commit\n\n[minor]").call()
    else commit().setMessage("New commit").call()
}

val Git.commitMajor: (release: ReleaseFromCommit) -> Unit get() = {
    if (it.value) commit().setMessage("New commit\n\n[major]").call()
    else commit().setMessage("New commit").call()
}
