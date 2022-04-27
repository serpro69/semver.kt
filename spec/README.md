## `io.github.serpro69:semver.kt-spec:$version`

[![Maven Central](https://img.shields.io/maven-central/v/io.github.serpro69/semver.kt-spec?style=for-the-badge)](https://search.maven.org/artifact/io.github.serpro69/semver.kt-spec)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/io.github.serpro69/semver.kt-spec?label=snapshot-version&server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge&color=yellow)](#downloading)

A kotlin implementation of the [semver](https://github.com/semver/semver) spec.

### Usage

#### Adding a dependency

gradle:

```kotlin
implementation("io.github.serpro69:semver.kt-spec:$version")
```

maven:

```xml
<dependency>
    <groupId>io.github.serpro69</groupId>
    <artifactId>semver.kt-spec</artifactId>
    <version>${version}</version>
</dependency>
```

#### Using `Semver` class

```kotlin
val semver = Semver("1.2.3")

// Incrementing a version
semver.incrementMajor() // 2.0.0
semver.incrementMinor() // 2.1.0
semver.incrementPatch() // 2.1.1
Semver("1.2.3-rc.4").incrementPreRelease() // 1.2.3-rc.5

// Comparing versions
println(semver > "0.0.1".toSemver()) // true

// Accessing version properties
with(Semver("1.2.3-rc.4+build.567")) {
    this.major // 1
    this.minor // 2
    this.patch // 3
    this.normalVersion // 1.2.3
    this.preRelease // rc.4
    this.buildMetadata // build.567
}
```
