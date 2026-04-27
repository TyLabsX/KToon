plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "KToon"

include(
    "ktoon-core",
    "ktoon-engine",
    "ktoon-parser",
    "ktoon-processing",
    "ktoon-writer",
    "ktoon-api",
    "ktoon-internal",
    "ktoon-codec",
    "ktoon-kotlinx"
)