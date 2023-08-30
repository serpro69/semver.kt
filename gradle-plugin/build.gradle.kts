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
}

gradlePlugin {
    testSourceSets(functionalTest)
}
