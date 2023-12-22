import org.gradle.api.tasks.testing.TestResult.ResultType
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
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
}

repositories {
    mavenCentral()
}

group = "io.github.serpro69"

subprojects {
    group = parent?.group?.toString() ?: "io.github.serpro69"
    val subProject = this@subprojects
    val projectArtifactId = "${rootProject.name}-${subProject.name}"
    val isGradlePlugin = subProject.name == "semantic-versioning"

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

    java {
        registerFeature("gradle7") {
            usingSourceSet(sourceSets.main.get())
            val p = this@subprojects
            capability(p.group.toString(), p.name, p.version.toString())
        }
    }

    configurations {
        matching { it.name != "detekt" && !it.name.contains("gradle7") }.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
                    useVersion(KOTLIN_VERSION)
                    because("All Kotlin modules should use the same version, and compiler uses $KOTLIN_VERSION")
                }
            }
        }
        matching { it.name != "detekt" && it.name.contains("gradle7") }.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
                    useVersion("1.8.22") // must be same as gradle7 feature dependency version for kotlin
                    because("All Kotlin modules should use the same version, and gradle 7.2 supports up to 1.8.22")
                }
            }
        }
        configureEach {
            if (isCanBeConsumed && name.startsWith("gradle7")) {
                attributes {
                    attribute(
                        GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                        objects.named("7.2")
                    )
                }
            } else if (isCanBeConsumed) { // default support is for gradle 8.+
                attributes {
                    attribute(
                        GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                        objects.named("8.0")
                    )
                }
            }
        }
    }

    dependencies {
        val gradle7Implementation by configurations
        implementation(kotlin("stdlib-jdk8"))
        gradle7Implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8") {
            version {
                strictly("1.8.22")
            }
        }
        gradle7Implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7") {
            version {
                strictly("1.8.22")
            }
        }
        gradle7Implementation("org.jetbrains.kotlin:kotlin-stdlib") {
            version {
                strictly("1.8.22")
            }
        }
        if (subProject.name in listOf("release", "semantic-versioning")) {
            val jgitVer = "6.8.0.202311291450-r"
            implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVer")
            implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVer")
            gradle7Implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVer")
            gradle7Implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVer")
        }

        // test
        testImplementation("io.kotest:kotest-runner-junit5-jvm:5.7.2")
        testImplementation("io.kotest:kotest-assertions-core-jvm:5.7.2")
        testImplementation("io.github.serpro69:kotlin-faker:1.15.0")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform {}

        testLogging {
            // set options for log level LIFECYCLE
            events = setOf(
                TestLogEvent.FAILED,
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_OUT
            )
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
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
            if (!version.toString().endsWith("SNAPSHOT") && !version.toString().startsWith("0.0.0")) {
                sign(publishing.publications[publicationName])
            }
        }

        tasks.withType<PublishToMavenRepository>().configureEach {
            val predicate = provider { !version.toString().startsWith("0.0.0") }
            onlyIf("New release") { predicate.get() }
        }

        tasks.withType<PublishToMavenLocal>().configureEach {
            val predicate = provider { version.toString().startsWith("0.0.0") }
            onlyIf("In development") { predicate.get() }
        }
    }

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
