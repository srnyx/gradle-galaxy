package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import xyz.srnyx.gradlegalaxy.extensions.minecraft.GenerateRuntimeLibraryEnumExtension


/** Configuration for `galaxy { annoyingAPI { customRuntimeLibraries { generateRuntimeLibraryEnum { } } } }` */
data class GenerateRuntimeLibraryEnumConfig(
    val relocateImports: Boolean = true,
    val packagePath: String? = null,
) {
    internal fun toExtension(): GenerateRuntimeLibraryEnumExtension.() -> Unit = {
        val config: GenerateRuntimeLibraryEnumConfig = this@GenerateRuntimeLibraryEnumConfig

        relocateImports.set(config.relocateImports)
        packagePath.set(config.packagePath)
    }
}
