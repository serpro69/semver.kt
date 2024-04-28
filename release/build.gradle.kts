plugins {
}

dependencies {
    /* :spec module version will be set to latest or next, since it's a multi-tag monorepo */
    api(project(":spec"))
    implementation("org.json:json:20240303")
}
