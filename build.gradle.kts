import io.github.serpro69.semverkt.gradle.plugin.tasks.TagTask
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KOTLIN_VERSION

plugins {
    java
    kotlin("jvm") version "1.9.21" apply false
    id("org.jetbrains.dokka") version "1.9.10"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0-rc-1"
    id("io.github.serpro69.semantic-versioning") apply false
}

repositories {
    mavenCentral()
}

group = "io.github.serpro69"

allprojects {
    configurations.matching { it.name != "detekt" }.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
                useVersion(KOTLIN_VERSION)
                because("All Kotlin modules should use the same version, and compiler uses $KOTLIN_VERSION")
            }
        }
    }
}

subprojects {
    group = parent?.group?.toString() ?: "io.github.serpro69"
    val subProject = this@subprojects
    val projectArtifactId = "${rootProject.name}-${subProject.name}"
    val isGradlePlugin = subProject.name == "semantic-versioning"

    val isSnapshot by lazy {
        provider {
            subProject.version.toString().startsWith("0.0.0")
                || subProject.version.toString().endsWith("SNAPSHOT")
        }
    }
    val newTag by lazy { provider { subProject.tasks.getByName("tag").didWork } }

    repositories {
        mavenCentral()
    }

    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.dokka")
        if (!isGradlePlugin) plugin("maven-publish")
        plugin("signing")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        testImplementation("io.kotest:kotest-runner-junit5-jvm:5.7.2")
        testImplementation("io.kotest:kotest-assertions-core-jvm:5.7.2")
        testImplementation("io.github.serpro69:kotlin-faker:1.15.0")

        if (subProject.name in listOf("release", "semantic-versioning")) {
//            val jgitVer = "6.8.0.202311291450-r"
            val jgitVer = "5.13.2.202306221912-r"
            implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVer")
            implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVer")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            // support gradle 7.5+
            languageVersion.set(KotlinVersion.KOTLIN_1_6)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform {}

        testLogging {
            // set options for log level LIFECYCLE
            events = setOf(
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
//                TestLogEvent.STANDARD_OUT
            )
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            displayGranularity = 2 // shorter test names
            // set options for log level DEBUG and INFO
            debug {
                events = setOf(
                    TestLogEvent.STARTED,
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_ERROR,
                    TestLogEvent.STANDARD_OUT
                )
                exceptionFormat = TestExceptionFormat.FULL
            }
            info.events = debug.events
            info.exceptionFormat = debug.exceptionFormat

            afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
                if (desc.parent == null) { // will match the outermost suite
                    val pass = "${Color.GREEN}${result.successfulTestCount} passed${Color.NONE}"
                    val fail = "${Color.RED}${result.failedTestCount} failed${Color.NONE}"
                    val skip = "${Color.YELLOW}${result.skippedTestCount} skipped${Color.NONE}"
                    val type = when (val r: ResultType = result.resultType) {
                        ResultType.SUCCESS -> "${Color.GREEN}$r${Color.NONE}"
                        ResultType.FAILURE -> "${Color.RED}$r${Color.NONE}"
                        ResultType.SKIPPED -> "${Color.YELLOW}$r${Color.NONE}"
                    }
                    val output = "Results: $type (${result.testCount} tests, $pass, $fail, $skip)"
                    val startItem = "|   "
                    val endItem = "   |"
                    val repeatLength = startItem.length + output.length + endItem.length - 36
                    println("")
                    println("\n" + ("-" * repeatLength) + "\n" + startItem + output + endItem + "\n" + ("-" * repeatLength))
                }
            }))
        }
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

    val publicationName = projectArtifactId
        .split(Regex("""[\.-]"""))
        .joinToString("") {
            it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
        }

    if (!isGradlePlugin) { // com.gradle.plugin-publish applies its own publishing config
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
            sign(publishing.publications[publicationName])
        }

        tasks.withType<PublishToMavenRepository>().configureEach {
            dependsOn(subProject.tasks.getByName("tag"))
            dependsOn(subProject.tasks.withType(Sign::class.java))
            onlyIf("Not snapshot") { !isSnapshot.get() }
            onlyIf("New tag") { newTag.get() }
        }

        tasks.withType<PublishToMavenLocal>().configureEach {
            onlyIf("In development") { isSnapshot.get() }
        }

        tasks.withType<Sign>().configureEach {
            dependsOn(subProject.tasks.getByName("tag"))
            onlyIf("Not snapshot") { !isSnapshot.get() }
            onlyIf("New tag") { newTag.get() }
        }
    }

    tasks.withType<TagTask>().configureEach {
        onlyIf("Not snapshot") { !isSnapshot.get() }
    }

    tasks.withType<DokkaTask>().configureEach {
        onlyIf("Not snapshot") { !isSnapshot.get() }
    }

    tasks {
        assemble {
            dependsOn(jar)
        }
    }
}

tasks.withType<TagTask>().configureEach {
    val isSnapshot = provider {
        version.toString().startsWith("0.0.0")
            || version.toString().endsWith("SNAPSHOT")
    }
    onlyIf("Not snapshot") { !isSnapshot.get() }
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

operator fun String.times(x: Int): String {
    return List(x) { this }.joinToString("")
}

internal enum class Color(ansiCode: Int) {
    NONE(0),
    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    PURPLE(35),
    CYAN(36),
    WHITE(37);

    private val ansiString: String = "\u001B[${ansiCode}m"

    override fun toString(): String {
        return ansiString
    }
}
