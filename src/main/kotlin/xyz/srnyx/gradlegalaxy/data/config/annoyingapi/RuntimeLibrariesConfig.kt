package xyz.srnyx.gradlegalaxy.data.config.annoyingapi


data class RuntimeLibrariesConfig(
    val addRepositories: Boolean = true,
    val addDependencies: Boolean = false,
    val relocate: Boolean = true,
)
