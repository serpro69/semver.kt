[![Stand With Ukraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner2-direct.svg)](https://stand-with-ukraine.pp.ua)

# `semver.kt`

<a href="https://github.com/serpro69/semver.kt"> <img src="docs/logo.png" alt="semver.kt"/> </a>

[![Build Status](https://img.shields.io/github/actions/workflow/status/serpro69/semver.kt/build.yml?branch=master&logo=github&style=for-the-badge)](https://github.com/serpro69/semver.kt/actions/workflows/build.yml)
[![Issues](https://img.shields.io/github/issues/serpro69/semver.kt.svg?logo=GitHub&style=for-the-badge&color=lightblue)](https://github.com/serpro69/semver.kt/issues)
![GitHub Top Lang](https://img.shields.io/github/languages/top/serpro69/semver.kt.svg?logo=Kotlin&logoColor=white&color=A97BFF&style=for-the-badge)
[![Awesome Kotlin](https://img.shields.io/badge/awesome-kotlin-orange?logo=Awesome-Lists&style=for-the-badge)](https://github.com/KotlinBy/awesome-kotlin)
[![Licence](https://img.shields.io/github/license/serpro69/semver.kt.svg?style=for-the-badge)](LICENSE.adoc)

***NB! This is still in pretty-much early development, thus breaking changes are to be expected until first stable release.***

## About

This repo contains three modules which can be used separately: 

- [`io.github.serpro69:semver.kt-spec`](spec) - a kotlin implementation of [semver](https://github.com/semver/semver) spec
- [`io.github.serpro69:semver.kt-release`](release) - provides functionality for automated release versioning (based on git tags) which adheres to semver rules
- [`io.github.serpro69.semantic-versioning`](semantic-versioning) a gradle plugin for automated semantic versioning during builds

## Why?

I've been using the [gradle-semantic-build-versioning](https://github.com/vivin/gradle-semantic-build-versioning) plugin for my gradle-based projects, and it works great for the most part. Unfortunately the project has not been maintained since late 2019, the author does not reply to open issues, and gradle has pretty strict rules on publishing forks of plugins. Therefore, I've decided to re-write my own plugin using Kotlin.

I like monorepo approach when it makes sense, so I decided to keep all the necessary code in one single repo. At the same time I think it could be useful to publish the "core" APIs (`semver-spec` and `semver-release`) as separate artifacts instead of having all the code within the plugin sources. This would allow to easily add a similar maven plugin in the future provided there's interest, using the same APIs. And at the same time these APIs could also be useful to someone else as standalone artifacts which can be used in other projects?

### Why not XXX?

I've found that alternatives do not really satisfy my needs and wants. For example, the popular [semantic-release](https://github.com/semantic-release/semantic-release) works great for JS projects, but is kind of a pain when used in JVM-based projects.

In addition, I want to expand on the idea of avoiding hardcoded versions in build files as this seems completely unnecessary since versions are also stored as git objects (tags, or sometimes branches). This is something that I don't see very often in other existing tools which tend to write versions to the build files, and which was one of the things that I really liked about the aforementioned 'gradle-semantic-build-versioning' plugin. 

## Usage

Each of the modules can be used separately either as a library by declaring a dependency or as a gradle plugin. For module-specific usage details see modules' respective README files.
