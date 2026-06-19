package xyz.srnyx.gradlegalaxy.data.config.annoyingapi


data class RuntimeLibrariesConfig(
    val addRepositories: Boolean = true,
    /**
     * Dependency classpaths to add the dependencies to (e.g. `compileOnly`, `implementation`, `testImplementation`, etc.).
     *
     * If empty, dependencies will not be added to any classpath.
     *
     * You usually don't need to change this as dependencies are on the `compileOnlyApi` classpath on Annoying API.
     */
    val configurations: List<String> = listOf("testImplementation"),
    val relocate: Boolean = true,
)
