plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":ktoon-core"))
    api(project(":ktoon-parser"))
    api(project(":ktoon-writer"))

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation(kotlin("test"))
}
