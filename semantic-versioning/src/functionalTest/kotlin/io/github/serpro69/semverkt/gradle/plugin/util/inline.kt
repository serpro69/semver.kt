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

@JvmInline
value class Release(val fromCommit: Boolean) {

    companion object {
        val ALL = listOf(Release(true), Release(false))

        inline fun forEach(action: (Release) -> Unit) = ALL.forEach(action)
    }

    val tag: ((proj: AbstractProject) -> (inc: (release: Release) -> Increment?) -> (dryRun: DryRun) -> BuildResult)
        get() = { project ->
            { inc ->
                { dryRun ->
                    val args = mutableListOf("tag", "-Prelease")
                    inc(this)?.let { args.add("-Pincrement=$it") }
                    if (dryRun.value) args.add("-PdryRun")
                    Builder.build(project = project, args = args.toTypedArray())
                }
            }
        }

    override fun toString(): String = fromCommit.toString()

    operator fun not(): Boolean = !fromCommit
}
