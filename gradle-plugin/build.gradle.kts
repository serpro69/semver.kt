plugins {
    `java-gradle-plugin`
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

dependencies {
    val integrationTestImplementation by configurations
    val functionalTestImplementation by configurations
    compileOnly(gradleApi())
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
    plugins {
        create("semver-release") {
            id = "${project.group}.${name}"
            implementationClass = "io.github.serpro69.semverkt.gradle.plugin.SemverKtPlugin"
        }
    }
    testSourceSets(sourceSets["functionalTest"])
}
