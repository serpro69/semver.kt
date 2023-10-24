# `io.github.serpro69.semantic-versioning:$version`

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.serpro69.semantic-versioning)](https://plugins.gradle.org/plugin/io.github.serpro69.semantic-versioning)

## Intro

Gradle settings plugin for semantic versioning releases.

This Gradle plugin provides support for [semantic versioning](http://semver.org) of gradle builds. It is easy to use and extremely configurable. The plugin allows you to bump the major, minor, patch or pre-release version based on the latest version (identified from a git tag). It's main advantage (and main motivation for creating this project) over other similar semantic-versioning projects is that it explicitly avoids to write versions to build files and only uses git tags, thus eliminating the "Release new version" noise from the git logs. 

The version can be bumped by using version-component-specific project properties, or alternatively based on the contents of a commit message. If no manual bumping is done via commit message or project property, the plugin will increment the version-component with the lowest precedence; this is usually the patch version, but can be the pre-release version if the latest version is a pre-release one. The plugin does its best to ensure that you do not accidentally violate semver rules while generating your versions; in cases where this might happen the plugin forces you to be explicit about violating these rules.

This is a settings plugin and is applied to `settings.gradle(.kts)`. Therefore, version calculation is performed right at the start of the build, before any projects are configured. This means that the project version is immediately available (almost as if it was set explicitly - which it effectively is), and will never change during the build (barring some other, external task that attempts to modify the version during the build). While the build is running, tagging or changing the project properties will not influence the version that was calculated at the start of the build.

_**Note**: The gradle documentation specifies that the version property is an `Object` instance. So to be absolutely safe, and especially if you might change versioning-plugins later, you should use the `toString()` method on `project.version`. However, this plugin does set the value of `project.version` to a `String` instance and hence you can treat it as such. While the version property is a string, it does expose some additional properties. These are `snapshot`, `major`, `minor`, `patch` and `preRelease`. `snapshot` is a boolean and can be used for release vs. snapshot project-configuration, instead of having to do an `endsWith()` check. `major`, `minor`, `patch` and `preRelease` bear the single version components for further usage in the build process. `major`, `minor` and `patch` are of type `int` and are always set, `preRelease` is a `String` and can be `null` if the current version is not a pre-release version._

## Usage

The latest version of this plugin can be found on [semantic-versioning gradle plugin](link) page. Using the plugin is quite simple:

**In `settings.gradle.kts`**

```kotlin
plugins {
    id("io.github.serpro69.semantic-versioning") version "$ver"
}
```

Additionally, you may want to add `semantic-versioning.json` configuration file in the corresponding project-directory of each project in the build that should be handled by this plugin. The file _must_ be in the same directory as `settings.gradle` file. This file allows you to set options to configure the plugin's behavior (see [Json Configuration](#json-configuration)). In most cases you don't want to version your subprojects separately from the main (root) project, and instead want to keep their versions in sync. For this you can simply set the version of each subproject to the version of the root project in the root project's `build.gradle.kts`:

```kotlin
subprojects {
    version = rootProject.version
}
```

This is usually enough to start using the plugin. Assuming that you already have tags that are (or contain) semantic versions, the plugin will search for all nearest ancestor-tags, select the latest<sup>1</sup> of them as the base version, and increment the component with the least precedence. The nearest ancestor-tags are those tags with a path between them and the `HEAD` commit, without any intervening tags. This is the default behavior of the plugin.

If you need the `TagTask` class in your Gradle build script, for example, for a construct like `tasks.withType(TagTask) { it.dependsOn publish }`, or when you want to define additional tag tasks, you can add the plugin's classes to the build script classpath by simply doing `plugins { id 'io.github.serpro69.gradle-semantic-build-versioning' version '3.0.4' apply false }`.

<sup>1</sup> Latest based on ordering-rules defined in the semantic-version specification, **not latest by date**.

## Configuration

### Json Configuration

TODO

### `semantic-versioning` Extension

The plugin provides a settings-extension called `semantic-versioning`, which, if used, takes precedence over the json-based configuration.

In the `settings.gradle.kts`, the extension can be configured as follows:

```kotlin
settings.extensions.configure<SemverPluginExtension>("semantic-versioning") {
    git {
        message {
            major = "<major>"
            minor = "<minor>"
            patch = "<patch>"
            ignoreCase = true
        }
    }
    version {
        defaultIncrement = Increment.MINOR
        preReleaseId = "rc"
    }
}
```

### Gradle Properties

Some options can also be overridden via gradle properties, namely the plugin makes use of the following properties:

- `promoteRelease` - boolean type property, promotes current pre-release to a release version
- `preRelease` - boolean type property, creates a new pre-release version from the current release
- `increment` - string type property, sets increment for the next version
  - setting increment via gradle property take precedence over json and plugin-extension configurations
  - accepted values are (case-insensitive):
    - `major`
    - `minor`
    - `patch`
    - `pre_release`

## Development

TODO

## Testing

To run all tests execute `./gradlew clean test functionalTest`, which will run both unit and functional tests.

### Testing from IDE

To run *functional* tests in an IDE, a gradle runner has to be used because gradle needs to generate `plugin-under-test-medatada.properties` file.

If running tests with gradle runner is not possible (I, for one, couldn't yet figure out how to do that with kotest tests in Intellij, even though I have set "Run tests using: Gradle" in Intellij's Build Tools -> Gradle settings), one could first generate the metadata with gradle by running `pluginUnderTestMedatata` task, and then execute tests in the IDE without cleaning the build directory.

### Manual Testing

To publish plugin and dependencies locally, run `./gradlew publishToMavenLocal publishAllPublicationsToLocalPluginRepoRepository`, which will publish dependencies to local maven directory (e.g. `~/.m2/repository`), and the plugin to `./build/local-plugin-repo`.

Once that's done, one can setup gradle to fetch the plugin from local sources by updating `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal() // needed to fetch dependencies of the plugin which were published locally
        mavenLocal {
            url = uri("/path/to/semver.kt/semantic-versioning/build/local-plugin-repo")
        }
    }
}

plugins {
    id("io.github.serpro69.semantic-versioning") version "0.0.0"
}

rootProject.name = "test"
```
