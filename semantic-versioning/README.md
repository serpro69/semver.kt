# `io.github.serpro69.semantic-versioning:$version`

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.serpro69.semantic-versioning?style=for-the-badge)](https://plugins.gradle.org/plugin/io.github.serpro69.semantic-versioning)

## Intro

Gradle settings plugin for semantic versioning releases.

This Gradle plugin provides support for [semantic versioning](http://semver.org) of gradle builds. It is easy to use and extremely configurable. The plugin allows you to bump the major, minor, patch or pre-release version based on the latest version (identified from a git tag). It's main advantage (and main motivation for creating this project) over other similar semantic-versioning projects is that it explicitly avoids to write versions to build files and only uses git tags, thus eliminating the "Release new version" noise from the git logs. 

The version can be bumped by using version-component-specific project properties, or alternatively based on the contents of a commit message. If no manual bumping is done via commit message or project property, the plugin will increment the version-component with the lowest precedence; this is usually the patch version, but can be the pre-release version if the latest version is a pre-release one. The plugin does its best to ensure that you do not accidentally violate semver rules while generating your versions; in cases where this might happen the plugin forces you to be explicit about violating these rules.

This is a settings plugin and is applied to `settings.gradle(.kts)`. Therefore, version calculation is performed right at the start of the build, before any projects are configured. This means that the project version is immediately available (almost as if it was set explicitly - which it effectively is), and will never change during the build (barring some other, external task that attempts to modify the version during the build). While the build is running, tagging or changing the project properties will not influence the version that was calculated at the start of the build.

_**Note**: The gradle documentation specifies that the version property is an `Object` instance. So to be absolutely safe, and especially if you might change versioning-plugins later, you should use the `toString()` method on `project.version`. However, this plugin does set the value of `project.version` to a `String` instance and hence you can treat it as such. While the version property is a string, it does expose some additional properties. These are `snapshot`, `major`, `minor`, `patch` and `preRelease`. `snapshot` is a boolean and can be used for release vs. snapshot project-configuration, instead of having to do an `endsWith()` check. `major`, `minor`, `patch` and `preRelease` bear the single version components for further usage in the build process. `major`, `minor` and `patch` are of type `int` and are always set, `preRelease` is a `String` and can be `null` if the current version is not a pre-release version._

## Usage

### Requirements

- gradle 7.5+

### Installation

The latest version of this plugin can be found on [semantic-versioning gradle plugin](https://plugins.gradle.org/plugin/io.github.serpro69.semantic-versioning) page.

***NB! While gradle makes a new release of a plugin available instantly, maven central takes some time to sync, and hence a new version of the plugin might not always work right away and report missing dependencies on the other submodules in this project.***

Using the plugin is quite simple:

**In `settings.gradle.kts`**

```kotlin
plugins {
    id("io.github.serpro69.semantic-versioning") version "$ver"
}
```

Additionally, you may want to add `semantic-versioning.json` configuration file in the corresponding project-directory of each project in the build that should be handled by this plugin. The file _must_ be in the same directory as `settings.gradle` file. This file allows you to set options to configure the plugin's behavior (see [Json Configuration](#json-configuration)).

In most cases you don't want to version your subprojects separately from the main (root) project, and instead want to keep their versions in sync. For this you can simply set the version of each subproject to the version of the root project in the root project's `build.gradle.kts`:

```kotlin
subprojects {
    version = rootProject.version
}
```

The plugin will still evaluate each project in a gradle build and will set the versions for each of the projects found at build configuration time.

This is usually enough to start using the plugin. Assuming that you already have tags that are (or contain) semantic versions, the plugin will search for all nearest ancestor-tags, select the latest<sup>1</sup> of them as the base version, and increment the component with the least precedence. The nearest ancestor-tags are those tags with a path between them and the `HEAD` commit, without any intervening tags. This is the default behavior of the plugin.

If you need the `TagTask` class in your Gradle build script, for example, for a construct like `tasks.withType(TagTask) { it.dependsOn publish }`, or when you want to define additional tag tasks, you can add the plugin's classes to the build script classpath by simply doing `plugins { id 'io.github.serpro69.semantic-versioning' version '$version' apply false }`.

<sup>1</sup> Latest based on ordering-rules defined in the semantic-version specification, **not latest by date**.

### Release Workflow

Incrementing new version can be done in one of the following ways, in ascending precedence order:

- default increment
- commit-based increment
- gradle property-based increment
- manually setting the version via `-Pversion`

#### Releasing from commits

A release based on the commit messages makes use of `git.message` configuration (see [Configuration](#configuration) section for more details), and looks up release keywords in square brackets (i.e `[minor]`) in commit messages between the current git HEAD (inclusive) and the latest version (exclusive).

For example, if the latest version is `1.0.0`, and one of the commits since then contains `[minor]` string, the next version will be `1.1.0`

Version precedence follows semver rules (`[major]` -> `[minor]` -> `[patch]` -> `[pre release]`), and if several commits contain a release keyword, then the highest precedence keyword will be used. For example, if there were 3 commits between latest version and current HEAD, and one of those commits contains `[minor]` keyword and another contains `[major]` keyword, the next version will be bumped with the major increment.

By default, releasing with uncommitted changes is not allowed and will fail the `:tag` task. This can be overridden by setting `git.repo.cleanRule` configuration property to `none`. The default rule will only consider tracked changes, and will allow releases with untracked files in the repository. To override this and only allow releases of "clean" repository, set the `cleanRule` property to `all` (See [Configuration](#configuration) section for more details.)

#### Releasing via gradle properties

Instead of having the plugin look up keywords in commits, one can use `-Pincrement` property instead with the `:tag` task.
See [Gradle Properties](#gradle-properties) for more details on available values.

As mentioned above, using the gradle property takes precedence over commit-based release via keywords.

## Configuration

### Json Configuration

The full config file looks like this:

```json
{
  "git": {
    "repo": {
      "directory": ".",
      "remoteName": "origin",
      "cleanRule": "tracked"
    },
    "tag": {
      "prefix": "v",
      "separator": "",
      "useBranches": "false"
    },
    "message": {
      "major": "major",
      "minor": "minor",
      "patch": "patch",
      "preRelease": "pre release",
      "ignoreCase": "true"
    }
  },
  "version": {
    "initialVersion": "0.1.0",
    "placeholderVersion": "0.0.0",
    "defaultIncrement": "minor",
    "preReleaseId": "rc",
    "initialPreRelease": "1",
    "snapshotSuffix": "SNAPSHOT"
  },
  "monorepo": {
    "sources": ".",
    "modules": [
      {
        "name": ":foo",
        "sources": "src/main",
        "tag": {
          "prefix": "foo-v",
          "separator": "",
          "useBranches": "false"
        }
      },
      {
        "name": ":bar",
        "sources": "."
      }
    ]
  }
}
```

Refer to [Configuration](../release/src/main/kotlin/io/github/serpro69/semverkt/release/configuration/Configuration.kt) class for kdocs on each available property.

- [ ] **TODO**: add kdocs and link them instead of referring to the file

### `semantic-versioning` Extension

The plugin provides a settings-extension called `semantic-versioning`, which, if used, takes precedence over the json-based configuration for any declared properties.

In the `settings.gradle.kts`, the extension can be configured as follows:

```kotlin
import io.github.serpro69.semverkt.gradle.plugin.SemverPluginExtension
import io.github.serpro69.semverkt.release.configuration.ModuleConfig
import io.github.serpro69.semverkt.release.configuration.TagPrefix
import kotlin.io.path.Path

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
    monorepo {
        sources = Path("src")
        module(":foo") {
            sources = "src/main"
            tag  {
                prefix = TagPrefix("foo-v")
                separator = ""
                useBranches = false
            }
        }
        module(":bar") {}
        modules.add(ModuleConfig(":baz"))
    }
}
```

### Gradle Properties

The plugin makes use of the following properties:

- `promoteRelease` - boolean type property, promotes current pre-release to a release version
- `preRelease` - boolean type property, creates a new pre-release version from the current release
- `increment` - string type property, sets increment for the next version
  - setting increment via gradle property take precedence over commit-based increments which can be configured via json and plugin-extension configurations
  - accepted values are (case-insensitive):
    - `major`
    - `minor`
    - `patch`
    - `pre_release`

### Monorepo Support

#### Single-Tag Monorepo

The plugin supports individual versioning of submodules (subprojects) of monorepo projects.
To configure a monorepo project, add the following configuration (also supported via json configuration):

```kotlin
import io.github.serpro69.semverkt.gradle.plugin.SemverPluginExtension
import kotlin.io.path.Path

settings.extensions.configure<SemverPluginExtension>("semantic-versioning") {
  monorepo {
    // path to track changes for the monorepo submodules that are not configured in this block
    // in this case it will be used for :baz project
    sources = Path(".")
    module(":foo") {}
    module(":bar") {
        // customize given module sources to track changes
        sources = Path("src/main")
    }
    module(":foo:bar")
  }
}

include("foo")
include("bar")
include("baz")
```

> [!NOTE]
> The module `path` should be a fully-qualified gradle project path.
> So for `./bar` module in the root of a gradle mono-repo, this would be `:bar`,
> and for `./foo/bar` module in a gradle mono-repo, this would be `:foo:bar`.

By default, the entire submodule directory is used to lookup changes. This can be customized via `sources` config property for a given submodule. In the above example, for `bar` module only changes to `bar/src/main` would be considered when making a new release. If no changes are detected between current git HEAD and last version in the repo, then the `version` property will not be applied to the submodule.

Root project and any submodule that is not included in the monorepo configuration are always versioned, regardless of detected changes. In the above example, `baz` submodule would always have a new version applied (if applicable according to [Release Workflow](#release-workflow) rules.)

By versioning submodules separately one can avoid publishing modules that do not contain any changes between current HEAD and last version, for example by configuring maven publication task as such:

```kotlin
tasks.withType<PublishToMavenRepository>().configureEach {
  val predicate = provider { version.toString() != "0.0.0" }
  onlyIf("new release") { predicate.get() }
}
```

(Read more about [conditional publishing](https://docs.gradle.org/current/userguide/publishing_customization.html#sec:publishing_maven:conditional_publishing) in official gradle docs.)

We use version `0.0.0` above as a "placeholder version" (exact value can be configured by modifying the `version.placeholderVersion` config property via plugin extension or json configuration), which is set in `gradle.properties` file in project's root directory:

```properties
# gradle.properties
version=0.0.0
```

Any module that does not have changes will not get the new version applied to it, and hence will stay on version `0.0.0` throughout the build process runtime (barring some external modifications to the `version` property), and hence this can be used in conditional checks to skip certain tasks for a given module.

This comes with some downsides which are good to be aware of when considering to version each submodule separately:

- the whole project is still versioned in git via tags and according to semver rules, however (configured) submodules are versioned individually
  - this could lead to confusions because git tag `v0.7.0` could potentially mean `foo:0.7.0` and at the same time `bar:0.6.0`
  - there will be "version jumps" for individual submodules, e.g. last version of `bar` was `0.6.0` and next is `0.8.0`

It can still be useful though, especially when each submodule has its own publishable artifacts. In such cases, more often than not one might not want to publish next version of an artifact that is exactly the same as the previous version.

#### Multi-Tag Monorepo

Since `v0.10.0`, the plugin also supports multi-tagging - each individual submodule *can* have a separate tag; it is also possible to mix and match, where one or more submodules follow the "root tag", and others have individual tags.

This can be useful to avoid some limitations of the single-tag monorepos, e.g. "version jumps".

Multi-Tag support is enabled when one or more modules declares a custom tag prefix via configuration, e.g. with settings extension:

```kotlin
settings.extensions.configure<SemverPluginExtension>("semantic-versioning") {
  monorepo {
    // path to track changes for the monorepo submodules that are not configured in this block
    // in this case it will be used for :baz project
    sources = Path(".")
    module(":foo") {}
    module(":bar") {
      // customize given module sources to track changes
      sources = Path("src/main")
      // modify tag configuration for the module
      tag {
        prefix = TagPrefix("bar-v")
      }
    }
    module(":foo:bar") {}
  }
}
```

#### Monorepo Versioning Workflow

For example monorepo versioning workflow diagrams refer to [single-tag_monorepo_workflow.png](../docs/images/single-tag_monorepo_workflow.png) and [multi-tag_monorepo_workflow.png](../docs/images/multi-tag_monorepo_workflow.png)

> [!IMPORTANT]
> In monorepo multi-tag projects, unlike single-tag monorepo and non-monorepo projects, each module will have a version applied to it. 
> For modules that don't have any changes between the latest version and the next release, the "latest version" will be set.
> 
> Single-tag monorepo project type does not support this as it would be impossible to determine "latest version" of a given module from a single git tag. 

> [!NOTE]
> These diagrams were made with [obsidian](https://obsidian.md). The original canvas file can be found in [docs/assets/monorepo_workflow.canvas](../docs/assets/monorepo_workflow.canvas)

## Development

TODO

## Testing

To run all tests execute `./gradlew clean test functionalTest`, which will run both unit and functional tests.

### Testing from IDE

To run *functional* tests in an IDE, a gradle runner has to be used because gradle needs to generate `plugin-under-test-medatada.properties` file.

If running tests with gradle runner is not possible (I, for one, couldn't yet figure out how to do that with kotest tests in Intellij, even though I have set "Run tests using: Gradle" in Intellij's Build Tools -> Gradle settings), one could first generate the metadata with gradle by running `pluginUnderTestMedatata` task, and then execute tests in the IDE without cleaning the build directory.

### Manual Testing

To publish plugin and dependencies locally, run `./gradlew publishToMavenLocal publishAllPublicationsToLocalPluginRepoRepository` (or use the `make local`), which will publish dependencies to local maven directory (e.g. `~/.m2/repository`), and the plugin to `./build/local-plugin-repo`.

Once that's done, one can set up gradle to fetch the plugin from local sources by updating `settings.gradle.kts`:

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
    id("io.github.serpro69.semantic-versioning") version "0.0.0-dev"
}

rootProject.name = "test"
```
