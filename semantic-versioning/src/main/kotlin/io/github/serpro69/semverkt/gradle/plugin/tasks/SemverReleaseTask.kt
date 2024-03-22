package io.github.serpro69.semverkt.gradle.plugin.tasks

import io.github.serpro69.semverkt.gradle.plugin.SemanticProject
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

abstract class SemverReleaseTask : DefaultTask() {

    @get:Internal
    internal abstract val semanticProject: Property<SemanticProject>
}
