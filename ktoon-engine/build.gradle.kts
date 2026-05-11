plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":ktoon-core"))
    implementation(project(":ktoon-parser"))
    implementation(project(":ktoon-processing"))
    implementation(project(":ktoon-writer"))
    implementation(project(":ktoon-internal"))

    testImplementation(kotlin("test"))
    testImplementation(project(":ktoon-core"))
    testImplementation(project(":ktoon-api"))
}
