plugins {
    kotlin("jvm")
}

dependencies {
    val jgitVer = "6.1.0.202203080745-r"

    implementation(project(":spec"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVer")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVer")
    implementation("dev.nohus:AutoKonfig:1.0.3")
}
