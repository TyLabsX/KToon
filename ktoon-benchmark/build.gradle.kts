plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    implementation(project(":ktoon-core"))
    implementation(project(":ktoon-parser"))
    implementation(project(":ktoon-kotlinx"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

application {
    mainClass.set("de.tylabsx.ktoon.kotlinx.benchmark.KToonEncodingBenchmarkKt")
}

tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Runs KToon encoding benchmarks."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(application.mainClass)
}
