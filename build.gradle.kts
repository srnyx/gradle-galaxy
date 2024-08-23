plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.20"
    id("com.gradle.plugin-publish") version "1.2.1"
    id("org.jetbrains.dokka") version "1.9.20"
}

group = "xyz.srnyx"
version = "1.2.3"
description = "A Gradle plugin to simplify the process of creating projects"
val projectId: String = "gradle-galaxy"
val vcs: String = "github.com/srnyx/$projectId"
val includeJavadocsSources: Boolean = true

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

// Set Kotlin JVM version
kotlin.jvmToolchain(8)

// Set Java version, text encoding, & docs/sources jar task dependencies
tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
    if (includeJavadocsSources) {
        dependsOn("javadocJar")
        dependsOn("sourcesJar")
    }
}

// Add docs and sources jars
if (includeJavadocsSources) sourceSets {
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

@Suppress("UnstableApiUsage")
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
        tags.set(listOf("srnyx", "minecraft", "spigot"))
    }
}

publishing.publications.create<MavenPublication>("pluginMaven") {
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
