plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "KToon"

val ktoonGroup = "de.tylabsx"
val ktoonVersion = "1.2.0"

gradle.beforeProject {
    group = ktoonGroup
    version = ktoonVersion
}

include(
    "ktoon-core",
    "ktoon-engine",
    "ktoon-parser",
    "ktoon-processing",
    "ktoon-writer",
    "ktoon-api",
    "ktoon-internal",
    "ktoon-codec",
    "ktoon-kotlinx",
    "ktoon-query",
    "ktoon-benchmark"
)
