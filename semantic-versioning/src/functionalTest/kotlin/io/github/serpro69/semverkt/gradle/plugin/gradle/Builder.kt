package io.github.serpro69.semverkt.gradle.plugin.gradle

import io.github.serpro69.semverkt.gradle.plugin.fixture.AbstractProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.io.PrintWriter
import java.nio.file.Path

object Builder {

    @JvmOverloads
    @JvmStatic
    fun build(
        gradleVersion: GradleVersion = GradleVersion.current(),
        project: AbstractProject,
        withPluginClasspath: Boolean = true,
        vararg args: String
    ): BuildResult = runner(gradleVersion, project.projectDir, withPluginClasspath, *args).build()

    @JvmOverloads
    @JvmStatic
    fun build(
        gradleVersion: GradleVersion = GradleVersion.current(),
        projectDir: Path,
        withPluginClasspath: Boolean = true,
        vararg args: String
    ): BuildResult = runner(gradleVersion, projectDir, withPluginClasspath, *args).build()

    @JvmOverloads
    @JvmStatic
    fun buildAndFail(
        gradleVersion: GradleVersion = GradleVersion.current(),
        project: AbstractProject,
        withPluginClasspath: Boolean = true,
        vararg args: String
    ): BuildResult = runner(
        gradleVersion,
        project.projectDir,
        withPluginClasspath,
        *args
    ).buildAndFail()

    @JvmOverloads
    @JvmStatic
    fun buildAndFail(
        gradleVersion: GradleVersion = GradleVersion.current(),
        projectDir: Path,
        withPluginClasspath: Boolean = true,
        vararg args: String
    ): BuildResult = runner(gradleVersion, projectDir, withPluginClasspath, *args).buildAndFail()

    private fun runner(
        gradleVersion: GradleVersion,
        projectDir: Path,
        withPluginClasspath: Boolean = true,
        vararg args: String
    ): GradleRunner = GradleRunner.create().apply {
        forwardOutput()
        forwardStdOutput(PrintWriter(System.out))
        forwardStdError(PrintWriter(System.err))
        if (withPluginClasspath) withPluginClasspath()
        withGradleVersion(gradleVersion.version)
        withProjectDir(projectDir.toFile())
        withArguments(*args, "-s")
        // disables gradle daemon , which causes out-of-memory when running multiple tests
        // https://discuss.gradle.org/t/testkit-how-to-turn-off-daemon/17843/2
        withDebug(true)
        // Ensure this value is true when `--debug-jvm` is passed to Gradle, and false otherwise
//        withDebug(getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0)
    }
}
