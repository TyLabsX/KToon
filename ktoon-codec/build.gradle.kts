plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":ktoon-core"))
    implementation(kotlin("reflect"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation(project(":ktoon-parser"))
    testImplementation(project(":ktoon-writer"))
    testImplementation(kotlin("test"))
}
