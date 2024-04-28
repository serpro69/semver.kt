import com.gradle.publish.PublishTask
import io.github.serpro69.semverkt.gradle.plugin.tasks.TagTask

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

dependencies {
    val integrationTestImplementation by configurations
    val functionalTestImplementation by configurations
    compileOnly(gradleApi())
    /* :release module version will be set to latest or next, since it's a multi-tag monorepo */
    api(project(":release"))
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
            tags = listOf(
                "semantic-release",
                "semantic-versioning",
                "semver-release",
                "semver",
                "release",
                "semantic",
                "versioning"
            )
            implementationClass = "io.github.serpro69.semverkt.gradle.plugin.SemverKtPlugin"
        }
    }
    testSourceSets(sourceSets["functionalTest"])
}

kotlin {
    compilerOptions {
        optIn.set(listOf("kotlin.RequiresOptIn"))
    }
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

val isSnapshot by lazy {
    provider {
        version.toString().startsWith("0.0.0")
            || version.toString().endsWith("SNAPSHOT")
    }
}
val newTag by lazy { provider { tasks.getByName("tag").didWork } }

tasks.withType<TagTask>().configureEach {
    onlyIf("Not snapshot") { !isSnapshot.get() }
}

tasks.withType<PublishTask>().configureEach {
    dependsOn(tasks.getByName("tag"))
    val predicate = provider {
        !isSnapshot.get()
            && newTag.get()
            && group == "Plugin Portal"
    }
    onlyIf("New release") { predicate.get() }
}

// workaround for https://github.com/gradle-nexus/publish-plugin/issues/84
tasks.withType<PublishToMavenRepository>().configureEach {
    val predicate = provider {
        isSnapshot.get() && repository.name == "localPluginRepo"
    }
    onlyIf("In development") { predicate.get() }
}

tasks.withType<PublishToMavenLocal>().configureEach {
    onlyIf("In development") { isSnapshot.get() }
}
