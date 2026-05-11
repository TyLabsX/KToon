plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":ktoon-core"))
    implementation(project(":ktoon-internal"))
    implementation(project(":ktoon-parser"))
    implementation(project(":ktoon-writer"))

    testImplementation(kotlin("test"))
    testImplementation(project(":ktoon-core"))
    testImplementation(project(":ktoon-api"))
}
