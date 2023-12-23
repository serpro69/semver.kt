import io.github.serpro69.semverkt.spec.Semver

plugins {
}

val spec = project.rootProject.subprojects.first { it.name == "spec" }
    ?: throw GradleException("spec project not found")

dependencies {
    /* :release and :spec are versioned separately
     * during development versions will always equal (both are set to a version placeholder via gradle.properties),
     * but during publishing they might not (depending on changes to a given module)
     * hence we check the versions equality and either set a dependency on a published :spec artifact
     * or a project-type dependency on the submodule
     */
    if (Semver(project.version.toString()) != (Semver(spec.version.toString()))) {
        // use latest version before next major
        api("io.github.serpro69:semver.kt-spec:[0.9.0,1.0.0)")
    } else {
        api(project(":spec"))
    }
    implementation("dev.nohus:AutoKonfig:1.1.0")
}
