plugins {
    kotlin("jvm")
}

group = "de.tylabsx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ktoon-core"))
    implementation(project(":ktoon-engine"))
    implementation(project(":ktoon-parser"))
    implementation(project(":ktoon-processing"))
    implementation(project(":ktoon-internal"))

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
