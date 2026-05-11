import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm") version "2.3.10" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
    `maven-publish`
}

repositories {
    mavenCentral()
}

subprojects {
    pluginManager.apply("java-library")
    pluginManager.apply("maven-publish")

    repositories {
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/TyLabsX/KToon")

            credentials {
                username = findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_ACTOR")

                password = findProperty("gpr.key") as String?
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(17)
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<JavaPluginExtension>("java") {
        withSourcesJar()
        withJavadocJar()
    }

    if (project.name != "ktoon-benchmark") {
        extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifactId = if (project.name == "ktoon-api") "ktoon" else project.name

                    pom {
                        name.set(if (project.name == "ktoon-api") "KToon" else project.name)
                        description.set("Kotlin-first library modules for the TOON format.")
                        url.set("https://github.com/TyLabsX/KToon")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                        developers {
                            developer {
                                id.set("tylabsx")
                                name.set("TyLabsX")
                                organization.set("TyLabsX")
                            }
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/TyLabsX/KToon")

                    credentials {
                        username = findProperty("gpr.user") as String?
                            ?: System.getenv("GITHUB_ACTOR")

                        password = findProperty("gpr.key") as String?
                            ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}
