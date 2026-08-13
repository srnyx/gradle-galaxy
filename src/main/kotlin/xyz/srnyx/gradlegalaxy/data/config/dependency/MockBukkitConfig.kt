package xyz.srnyx.gradlegalaxy.data.config.dependency

import xyz.srnyx.gradlegalaxy.extensions.testing.MockBukkitExtension


/** Configuration for `galaxy { mockBukkit { } }` */
open class MockBukkitConfig(
    var group: String = "com.github.seeseemelk",
    var minecraftVersion: String = "1.20",
) {
    fun toExtension(): MockBukkitExtension.() -> Unit = {
        val config: MockBukkitConfig = this@MockBukkitConfig

        group = config.group
        minecraftVersion.set(config.minecraftVersion)
    }
}
