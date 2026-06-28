# Gradle Galaxy [![Release](https://img.shields.io/gradle-plugin-portal/v/xyz.srnyx.gradle-galaxy?label=Release%20(Gradle%20Plugin%20Portal)&color=006d82)](https://plugins.gradle.org/plugin/xyz.srnyx.gradle-galaxy) [![Snapshot](https://repo.srnyx.com/api/badge/latest/snapshots/xyz/srnyx/gradle-galaxy?color=006d82&name=Snapshot%20(repo.srnyx.com))](https://repo.srnyx.com/#/snapshots/xyz/srnyx/gradle-galaxy)

A Gradle plugin to simplify the process of creating projects. Thank you [dkim19375](https://github.com/dkim19375) for the help with some stuff :)

## Installation

You can install the plugin just like any other Gradle plugin by adding it to your plugins block. Make sure to replace `VERSION` with your [desired version](https://plugins.gradle.org/plugin/xyz.srnyx.gradle-galaxy). *Currently only supports Kotlin DSL!*

```kotlin
plugins {
    id("xyz.srnyx.gradle-galaxy") version "VERSION"
}
```

<details>
  <summary>Adding repository for snapshots</summary>
  
  Add this to your `settings.gradle.kts`:
  
  ```kts
  pluginManagement.repositories {
      maven("https://repo.srnyx.com/snapshots/")
      gradlePluginPortal() // For other plugins
  }
  ```
</details>

## Documentation

For the full documentation, please go here: [github.com/srnyx/gradle-galaxy/wiki](https://github.com/srnyx/gradle-galaxy/wiki)
