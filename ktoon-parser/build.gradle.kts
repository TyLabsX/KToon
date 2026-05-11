plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":ktoon-core"))
    implementation(project(":ktoon-internal"))

    testImplementation(kotlin("test"))
    testImplementation(project(":ktoon-core"))
}
