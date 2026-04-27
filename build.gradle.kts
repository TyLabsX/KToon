import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.10" apply false
    java
}

group = "de.tylabsx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    group = "de.tylabsx"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}