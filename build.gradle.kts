import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.9.10" apply false
    id("org.jetbrains.dokka") version "1.8.20"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0-rc-1"
}

repositories {
    mavenCentral()
}

group = "io.github.serpro69"

subprojects {
    group = parent?.group?.toString() ?: "io.github.serpro69"
    version = rootProject.version
    val subProject = this@subprojects
    val projectArtifactId = "${rootProject.name}-${subProject.name}"
    val isGradlePlugin = subProject.name == "gradle-plugin"

    repositories {
        mavenCentral()
    }

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.dokka")
//        if (!isGradlePlugin) {
            plugin("signing")
            plugin("maven-publish")
//        }
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        testImplementation("io.kotest:kotest-runner-junit5-jvm:5.6.2")
        testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
        testImplementation("io.github.serpro69:kotlin-faker:1.14.0")

        if (subProject.name in listOf("gradle-plugin", "release")) {
            val jgitVer = "5.13.0.202109080827-r"
            implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVer")
            implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVer")
        }
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
        archiveBaseName.set(projectArtifactId)

        afterEvaluate { // allow declaring additional dependencies in subproject's build.gradle file
            manifest {
                attributes(
                    mapOf(
                        "Implementation-Title" to projectArtifactId,
                        "Implementation-Version" to subProject.version,
                        "Class-Path" to configurations.compileClasspath.get().joinToString(" ") { it.name }
                    )
                )
            }
        }
    }

    val sourcesJar by tasks.creating(Jar::class) {
        archiveBaseName.set(projectArtifactId)
        archiveClassifier.set("sources")
        from(sourceSets.getByName("main").allSource)
        from("LICENCE.md") {
            into("META-INF")
        }
    }

    val dokkaJavadocJar by tasks.creating(Jar::class) {
        archiveBaseName.set(projectArtifactId)
        archiveClassifier.set("javadoc")
        dependsOn(tasks.dokkaJavadoc)
        from(tasks.dokkaJavadoc.get().outputDirectory.orNull)
    }

    artifacts {
        archives(sourcesJar)
        archives(dokkaJavadocJar)
    }

    val artifactGroup = subProject.group.toString()
    val artifactVersion = subProject.version.toString()
    val releaseTagName = "v$artifactVersion"

    val pomUrl = "https://github.com/serpro69/${rootProject.name}"
    val pomScmUrl = "https://github.com/serpro69/${rootProject.name}"
    val pomIssueUrl = "https://github.com/serpro69/${rootProject.name}/issues"
    val pomDesc = "https://github.com/serpro69/${rootProject.name}"

    val ghRepo = "serpro69/${rootProject.name}"
    val ghReadme = "README.md"

    val pomLicenseName = "MIT"
    val pomLicenseUrl = "https://opensource.org/licenses/mit-license.php"
    val pomLicenseDist = "repo"

    val pomDeveloperId = "serpro69"
    val pomDeveloperName = "Serhii Prodan"

    val publicationName = projectArtifactId.split(Regex("""[\.-]""")).joinToString("") { it.capitalize() }
//    if (!isGradlePlugin) {
        publishing {
            publications {
                create<MavenPublication>(publicationName) {
                    groupId = artifactGroup
                    artifactId = projectArtifactId
                    version = artifactVersion
                    from(components["java"])
                    artifact(sourcesJar)
                    artifact(dokkaJavadocJar)

                    pom {
                        packaging = "jar"
                        name.set(projectArtifactId)
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
                sign(publishing.publications[publicationName])
            }
        }
//    }

    tasks {
        assemble {
            dependsOn(jar)
        }
    }
}

val jar by tasks.getting(Jar::class) {
    enabled = false // nothing to build in root project
}

nexusPublishing {
    this@nexusPublishing.repositories {
        sonatype {
            stagingProfileId.set(properties["stagingProfileId"]?.toString())
        }
    }
}
