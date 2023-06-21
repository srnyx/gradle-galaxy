import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm") version "1.8.22"
    id("com.gradle.plugin-publish") version "1.2.0"
    id("org.jetbrains.dokka") version "1.8.20"
}

group = "xyz.srnyx"
version = "1.0.1"
description = "A Gradle plugin to simplify the process of creating projects"
val pluginName = "Gradle Galaxy"
val vcs: String = "github.com/srnyx/${project.name}"

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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

// Add docs and sources jars
sourceSets {
    val dokkaTask = tasks.getByName("dokkaHtml")
    tasks.create("javadocJar", Jar::class) {
        dependsOn(dokkaTask)
        from(dokkaTask)
        archiveClassifier.set("javadoc")
    }
    tasks.create("sourcesJar", Jar::class) {
        from(sourceSets["main"].allSource.srcDirs)
        archiveClassifier.set("sources")
    }
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
            displayName = pluginName
            description = project.description
            tags.set(listOf("srnyx", "minecraft", "spigot"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            pom {
                name.set(pluginName)
                description.set(project.description)
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
                        timezone.set("America/New_York")
                        organization.set("Venox Network")
                        organizationUrl.set("https://venox.network")
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
