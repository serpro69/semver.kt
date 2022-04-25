plugins {
    kotlin("jvm")
}

dependencies {
    val jgitVer = "5.13.0.202109080827-r"

    api(project(":spec"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVer")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVer")
    implementation("dev.nohus:AutoKonfig:1.0.4")
}
