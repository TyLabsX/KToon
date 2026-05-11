plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":ktoon-core"))
    api(project(":ktoon-engine"))
    api(project(":ktoon-parser"))
    api(project(":ktoon-writer"))
    api(project(":ktoon-processing"))
    api(project(":ktoon-codec"))
    api(project(":ktoon-kotlinx"))
    api(project(":ktoon-query"))

    implementation(project(":ktoon-internal"))

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")

    testImplementation(kotlin("test"))
    testImplementation(project(":ktoon-core"))
}
