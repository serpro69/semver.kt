import io.github.serpro69.semverkt.spec.Semver

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

val integrationTest: SourceSet by sourceSets.creating
val integrationTestImplementation by configurations
val functionalTest: SourceSet by sourceSets.creating
val functionalTestImplementation by configurations

sourceSets {
    listOf(integrationTest, functionalTest).map { it.name }.forEach {
        getByName(it) {
            resources.srcDir("src/$it/resources")
            compileClasspath += main.get().compileClasspath + test.get().compileClasspath
            runtimeClasspath += main.get().runtimeClasspath + test.get().runtimeClasspath
        }
    }
}

configurations {
    getByName("integrationTestImplementation") { extendsFrom(testImplementation.get()) }
    getByName("integrationTestRuntimeOnly") { extendsFrom(testRuntimeOnly.get()) }
    getByName("functionalTestImplementation") { extendsFrom(testImplementation.get()) }
    getByName("functionalTestRuntimeOnly") { extendsFrom(testRuntimeOnly.get()) }
}

val release = project.rootProject.subprojects.first { it.name == "release" }
    ?: throw GradleException("release project not found")

dependencies {
    val integrationTestImplementation by configurations
    val functionalTestImplementation by configurations
    compileOnly(gradleApi())
    /* :release and :semantic-versioning are versioned separately
     * during development versions will always equal (both are set to a version placeholder via gradle.properties),
     * but during publishing they might not (depending on changes to a given module)
     * hence we check the versions equality and either set a dependency on a published :spec artifact
     * or a project-type dependency on the submodule
     */
    if (Semver(project.version.toString()) != (Semver(release.version.toString()))) {
        // use latest version before next major
        api("io.github.serpro69:semver.kt-release:[0.7.0,1.0.0)")
    } else {
        api(project(":release"))
    }
    testCompileOnly(gradleTestKit())
    integrationTestImplementation(project)
    functionalTestImplementation(project)
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    group = "verification"
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.check {
    dependsOn(functionalTestTask)
}

gradlePlugin {
    website = "https://github.com/serpro69/semver.kt"
    vcsUrl = "https://github.com/serpro69/semver.kt.git"
    plugins {
        create(project.name) {
            id = "${project.group}.${name}"
            displayName = "Automated semantic versioning of gradle projects through git tags"
            description = "This plugin helps you to automatically version your gradle project according to semver rules"
            tags = listOf("semver", "release", "semantic", "versioning", "semantic-release", "semver-release")
            implementationClass = "io.github.serpro69.semverkt.gradle.plugin.SemverKtPlugin"
        }
    }
    testSourceSets(sourceSets["functionalTest"])
}

tasks["functionalTest"].dependsOn("pluginUnderTestMetadata")

publishing {
    repositories {
        maven {
            name = "localPluginRepo"
            url = uri("./build/local-plugin-repo")
        }
    }
}

// workaround for https://github.com/gradle-nexus/publish-plugin/issues/84
tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        !repository.name.contains("sonatype", ignoreCase = true)
    }
}
