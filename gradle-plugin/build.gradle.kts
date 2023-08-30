plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

val integrationTest: SourceSet by sourceSets.creating
val integrationTestImplementation by configurations
val functionalTest: SourceSet by sourceSets.creating
val functionalTestImplementation by configurations

dependencies {
    compileOnly(gradleApi())
    api(project(":release"))
    testCompileOnly(gradleTestKit())
    integrationTestImplementation(project)
    functionalTestImplementation(project)
    functionalTestImplementation("io.kotest:kotest-runner-junit5-jvm:5.3.0")
    functionalTestImplementation("io.kotest:kotest-assertions-core-jvm:5.3.0")
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.check {
    dependsOn(functionalTestTask)
}

gradlePlugin {
    plugins {
        create("semver-release") {
            id = "${project.group}.${name}"
            implementationClass = "io.github.serpro69.semverkt.gradle.plugin.SemverKtPlugin"
        }
    }
    testSourceSets(functionalTest)
}
