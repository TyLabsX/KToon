plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ktoon-core"))
    implementation(project(":ktoon-engine"))
    implementation(project(":ktoon-parser"))
    implementation(project(":ktoon-processing"))
    implementation(project(":ktoon-internal"))
    implementation(project(":ktoon-codec"))
    api(project(":ktoon-kotlinx"))

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")

    testImplementation(kotlin("test"))
    testImplementation(project(":ktoon-core"))
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}
