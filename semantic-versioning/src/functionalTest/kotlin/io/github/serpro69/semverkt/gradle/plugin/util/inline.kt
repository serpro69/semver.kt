package io.github.serpro69.semverkt.gradle.plugin.util

import io.github.serpro69.semverkt.gradle.plugin.fixture.AbstractProject
import io.github.serpro69.semverkt.gradle.plugin.gradle.Builder
import io.github.serpro69.semverkt.release.Increment
import org.gradle.testkit.runner.BuildResult

@JvmInline
value class DryRun(val value: Boolean) {

    companion object {
        val ALL = listOf(DryRun(true), DryRun(false))

        inline fun forEach(action: (DryRun) -> Unit) = ALL.forEach(action)
    }

    override fun toString(): String = value.toString()

    operator fun not(): Boolean = !value
}

val fromCommit = ReleaseFromCommit(true)
val fromProperty = ReleaseFromCommit(false)

@JvmInline
value class ReleaseFromCommit(val value: Boolean) {

    companion object {
        val ALL = listOf(ReleaseFromCommit(true), ReleaseFromCommit(false))

        val preRelease: (ReleaseFromCommit) -> Increment? = { if (it.value) null else Increment.PRE_RELEASE }
        val patch: (ReleaseFromCommit) -> Increment? = { if (it.value) null else Increment.PATCH }
        val minor: (ReleaseFromCommit) -> Increment? = { if (it.value) null else Increment.MINOR }
        val major: (ReleaseFromCommit) -> Increment? = { if (it.value) null else Increment.MAJOR }

        inline fun forEach(action: (ReleaseFromCommit) -> Unit) = ALL.forEach(action)
    }

    val tag: ((proj: AbstractProject) -> (inc: (release: ReleaseFromCommit) -> Increment?) -> (dryRun: DryRun) -> BuildResult)
        get() = { project ->
            { inc ->
                { dryRun ->
                    val args = mutableListOf("tag")
                    inc(this)?.let {
                        args.add("-Pincrement=$it")
                        args.add("-Prelease")
                    }
                    if (dryRun.value) args.add("-PdryRun")
                    Builder.build(project = project, args = args.toTypedArray())
                }
            }
        }

    override fun toString(): String = value.toString()

    operator fun not(): Boolean = !value
}

@JvmInline
value class UpToDate(val value: Boolean) {

    companion object {
        val TRUE = UpToDate(true)
        val FALSE = UpToDate(false)
    }

    override fun toString(): String = value.toString()

    operator fun not(): Boolean = !value
}
