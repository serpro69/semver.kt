import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.5.31"
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

group = "io.github.serpro69"

dependencies {
    implementation(kotlin("bom"))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.0.2")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.0.2")
    testImplementation("io.kotest:kotest-property-jvm:5.0.2")
    testImplementation("io.github.serpro69:kotlin-faker:1.10.0")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform {}
}

val jar by tasks.getting(Jar::class) {
    archiveBaseName.set(rootProject.name)

    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Class-Path" to configurations.compileClasspath.get().joinToString(" ") { it.name }
            )
        )
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
    from("LICENCE.md") {
        into("META-INF")
    }
}

val dokkaJavadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.get().outputDirectory.orNull)
}

artifacts {
    archives(sourcesJar)
    archives(dokkaJavadocJar)
}

val artifactName = rootProject.name
val artifactGroup = project.group.toString()
val artifactVersion = project.version.toString()
val releaseTagName = "v$artifactVersion"

val pomUrl = "https://github.com/serpro69/Semver.kt"
val pomScmUrl = "https://github.com/serpro69/Semver.kt"
val pomIssueUrl = "https://github.com/serpro69/Semver.kt/issues"
val pomDesc = "https://github.com/serpro69/Semver.kt"

val ghRepo = "serpro69/Semver.kt"
val ghReadme = "README.md"

val pomLicenseName = "MIT"
val pomLicenseUrl = "https://opensource.org/licenses/mit-license.php"
val pomLicenseDist = "repo"

val pomDeveloperId = "serpro69"
val pomDeveloperName = "Serhii Prodan"

publishing {
    publications {
        create<MavenPublication>("semver") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            from(components["java"])
            artifact(jar)
            artifact(sourcesJar)
            artifact(dokkaJavadocJar)

            pom {
                packaging = "jar"
                name.set(rootProject.name)
                description.set(pomDesc)
                url.set(pomUrl)
                scm {
                    url.set(pomScmUrl)
                }
                issueManagement {
                    url.set(pomIssueUrl)
                }
                licenses {
                    license {
                        name.set(pomLicenseName)
                        url.set(pomLicenseUrl)
                    }
                }
                developers {
                    developer {
                        id.set(pomDeveloperId)
                        name.set(pomDeveloperName)
                    }
                }
            }
        }
    }
}

signing {
    if (!version.toString().endsWith("SNAPSHOT")) {
        sign(publishing.publications["semver"])
    }
}

tasks {
    assemble {
        dependsOn(jar)
    }
}
