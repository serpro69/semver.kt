package io.github.serpro69.semverkt.gradle.plugin.fixture

import org.eclipse.jgit.api.Git
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

abstract class AbstractProject : AutoCloseable {

    val projectDir = Path("build/functionalTest/${slug()}").createDirectories().also {
        it.resolve("README.md").createFile().writeText("Hello, world")
        Git.init().setDirectory(it.toFile()).call()
        Git.open(it.toFile()).use { git ->
            git.add().addFilepattern("README.md").call()
            git.commit().setMessage("init").call()
        }
    }
    private val buildDir = projectDir.resolve("build")

    fun buildFile(filename: String): Path {
        return buildDir.resolve(filename)
    }

    private fun slug(): String {
        val worker = System.getProperty("org.gradle.test.worker")?.let { w -> "-$w" }.orEmpty()
        return "${javaClass.simpleName}-${UUID.randomUUID().toString().take(16)}$worker"
    }

    override fun close() {
        projectDir.toFile().deleteRecursively()
    }
}
