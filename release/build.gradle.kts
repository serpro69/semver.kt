import io.github.serpro69.semverkt.spec.Semver

plugins {
}

val spec = project.rootProject.subprojects.first { it.name == "spec" }
    ?: throw GradleException("spec project not found")

dependencies {
    val gradle7Api by configurations
    val gradle7Implementation by configurations

    /* :release and :spec are versioned separately
     * during development versions will always equal (both are set to a version placeholder via gradle.properties),
     * but during publishing they might not (depending on changes to a given module)
     * hence we check the versions equality and either set a dependency on a published :spec artifact
     * or a project-type dependency on the submodule
     */
    if (Semver(project.version.toString()) != (Semver(spec.version.toString()))) {
        // use latest version before next major
        api("io.github.serpro69:semver.kt-spec:[0.7.0,1.0.0)")
        gradle7Api("io.github.serpro69:semver.kt-spec:[0.7.0,1.0.0)") {
            capabilities {
                requireCapability("${project.group}:spec-gradle7")
            }
        }
    } else {
        api(project(":spec"))
        gradle7Api(project(":spec")) {
            capabilities {
                requireCapability("${project.group}:spec-gradle7")
            }
        }
    }
    implementation("dev.nohus:AutoKonfig:1.1.0")
    gradle7Implementation("dev.nohus:AutoKonfig:1.1.0")
}
