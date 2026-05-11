plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":ktoon-core"))

    testImplementation(kotlin("test"))
    testImplementation(project(":ktoon-core"))
}
