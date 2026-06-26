plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.24" // Do not update
    kotlin("plugin.serialization") version "1.9.24" // Do not update
    id("com.gradle.plugin-publish") version "1.3.1"
    id("org.jetbrains.dokka") version "1.9.20" // Do not update because of kotlin("jvm") version
}

group = "xyz.srnyx"
version = providers.environmentVariable("VERSION").orNull?.takeIf(String::isNotBlank) ?: "3.1.0"
description = "A Gradle plugin to simplify the process of creating projects"
val projectId: String = "gradle-galaxy"
val vcs: String = "github.com/srnyx/$projectId"
val includeJavadocsSources: Boolean = true
val javaVersion = 17

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Plugins
    compileOnly("com.gradleup.shadow:shadow-gradle-plugin:9.0.0")
    compileOnly("me.modmuss50.mod-publish-plugin:me.modmuss50.mod-publish-plugin.gradle.plugin:2.1.1")
}

// Set Kotlin JVM version
kotlin.jvmToolchain(javaVersion)

// Set Java version, text encoding, & docs/sources jar task dependencies
tasks.withType<JavaCompile> {
    sourceCompatibility = javaVersion.toString()
    targetCompatibility = javaVersion.toString()
    options.encoding = "UTF-8"
}

// Add docs and sources jars
if (includeJavadocsSources) {
    tasks.register("javadocJar", Jar::class) {
        group = "build"
        description = "Assembles a jar archive containing the javadoc files"
        val dokkaTask = tasks.getByName("dokkaHtml")
        dependsOn(dokkaTask)
        from(dokkaTask)
        archiveClassifier.set("javadoc")
    }
    tasks.register("sourcesJar", Jar::class) {
        group = "build"
        description = "Assembles a jar archive containing the sources"
        from(sourceSets["main"].allSource.srcDirs)
        archiveClassifier.set("sources")
    }
}

gradlePlugin {
    isAutomatedPublishing = true
    website.set("https://${vcs}")
    vcsUrl.set("https://${vcs}")
    plugins.create(projectId) {
        id = "${project.group}.$projectId"
        implementationClass = "${project.group}.gradlegalaxy.GradleGalaxy"
        version = project.version
        displayName = project.name
        description = project.description
        tags.set(listOf("srnyx", "minecraft", "spigot", "paper", "adventure", "jda"))
    }
}

publishing {
    publications.create<MavenPublication>("pluginMaven") {
        artifactId = projectId
        pom {
            name.set(project.name)
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

    // Custom/additional repository
    providers.environmentVariable("MAVEN_URL")
        .orNull
        ?.takeIf(String::isNotBlank)
        ?.let { mavenUrl -> repositories.maven {
            name = "srnyx"
            url = uri(mavenUrl)

            // Username/password
            val mavenName = providers.environmentVariable("MAVEN_NAME")
                .orNull
                ?.takeIf(String::isNotBlank)
            val mavenSecret = providers.environmentVariable("MAVEN_SECRET")
                .orNull
                ?.takeIf(String::isNotBlank)
            if (mavenName != null && mavenSecret != null) credentials {
                username = mavenName
                password = mavenSecret
            }
        } }
}
