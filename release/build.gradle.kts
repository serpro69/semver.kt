import io.github.serpro69.semverkt.spec.Semver

plugins {
}

val spec = rootProject.subprojects.first { it.path == ":spec" }
    ?: throw GradleException("spec project not found")

dependencies {
    /* :release and :spec are versioned separately
     * during development versions will always equal (both are set to a version placeholder via gradle.properties),
     * but during publishing they might not (depending on changes to a given module)
     * hence we check the versions equality and either set a dependency on a published :spec artifact with latest version
     * (latest is handled by plugin for multi-tag monorepos),
     * or a project-type dependency on the submodule
     */
    if (Semver(project.version.toString()) != (Semver(spec.version.toString()))) {
        api("io.github.serpro69:semver.kt-spec:${spec.version}")
    } else {
        api(project(":spec"))
    }
    implementation("org.json:json:20240303")
}
