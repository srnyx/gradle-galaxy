import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm") version "1.8.22"
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = "xyz.srnyx"
version = "1.0.0"
val pluginDescription: String = "A Gradle plugin to simplify the process of creating projects"
val vcs: String = "github.com/srnyx/${project.name}"

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8"))

    // Plugins
    compileOnly("com.github.jengelman.gradle.plugins", "shadow", "6.1.0")
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    isAutomatedPublishing = true
    website.set("https://${vcs}")
    vcsUrl.set("https://${vcs}")
    plugins {
        create(project.name) {
            id = "${project.group}.${project.name}"
            implementationClass = "${project.group}.gradlegalaxy.GradleGalaxy"
            version = project.version
            displayName = "Gradle Galaxy Plugin"
            description = pluginDescription
            tags.set(listOf("srnyx", "minecraft", "spigot"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            pom {
                name.set("Gradle Galaxy")
                description.set(pluginDescription)
                url.set("https://${vcs}")
                packaging = "jar"

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("srnyx")
                        url.set("https://srnyx.com")
                        email.set("contact@srnyx.com")
                        organization.set("Venox Network")
                        organizationUrl.set("https://venox.network")
                        timezone.set("America/New_York")
                    }
                    developer {
                        id.set("dkim19375")
                        timezone.set("America/New_York")
                        roles.add("contributor")
                    }
                }

                scm {
                    connection.set("scm:git:git://${vcs}.git")
                    developerConnection.set("scm:git:ssh://${vcs}.git")
                    url.set("https://${vcs}")
                }
            }
        }
    }
}
