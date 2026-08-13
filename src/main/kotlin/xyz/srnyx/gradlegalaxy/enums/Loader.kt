package xyz.srnyx.gradlegalaxy.enums


enum class Loader(val parent: Loader? = null) {
    BUKKIT,
    SPIGOT(BUKKIT),
    PAPER(SPIGOT),
    PURPUR(PAPER),
    FOLIA(PAPER),;

    fun getParents() = generateSequence(this) { it.parent }.toList()

    fun getModPublishPluginName() = toString().lowercase()

    companion object {
        fun getSupportedLoaders(loaders: List<Loader>) = entries.filter { loader -> loaders.any { it.getParents().contains(loader) } }
    }
}
