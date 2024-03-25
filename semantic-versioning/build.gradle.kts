import com.gradle.publish.PublishTask
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

configurations.configureEach {
    if (isCanBeConsumed) {
        attributes {
            attribute(
                GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                objects.named("7.5")
            )
        }
    }
}

val release = rootProject.subprojects.first { it.path == ":release" }
    ?: throw GradleException("release project not found")

dependencies {
    val integrationTestImplementation by configurations
    val functionalTestImplementation by configurations
    compileOnly(gradleApi())
    /* :release and :semantic-versioning are versioned separately
     * during development versions will always equal (both are set to a version placeholder via gradle.properties),
     * but during publishing they might not (depending on changes to a given module)
     * hence we check the versions equality and either set a dependency on a published :release artifact with latest version
     * (latest is handled by plugin for multi-tag monorepos),
     * or a project-type dependency on the submodule
     */
    if (Semver(project.version.toString()) != (Semver(release.version.toString()))) {
        api("io.github.serpro69:semver.kt-release:${release.version}")
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

tasks.withType<PublishTask>().configureEach {
    val predicate = provider {
        !version.toString().startsWith("0.0.0")
            && group == "Plugin Portal"
    }
    onlyIf("New release") { predicate.get() }
}

// workaround for https://github.com/gradle-nexus/publish-plugin/issues/84
tasks.withType<PublishToMavenRepository>().configureEach {
    val predicate = provider {
        version.toString().startsWith("0.0.0")
            && repository.name == "localPluginRepo"
    }
    onlyIf("In development") { predicate.get() }
}

tasks.withType<PublishToMavenLocal>().configureEach {
    val predicate = provider { version.toString().startsWith("0.0.0") }
    onlyIf("In development") { predicate.get() }
}
