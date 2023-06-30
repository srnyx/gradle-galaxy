package xyz.srnyx.gradlegalaxy.enums


/**
 * A parent interface for all components
 */
fun interface Component {
    /**
     * Returns the full component name used for the dependency notation
     */
    fun getComponent(): String
}

/**
 * `adventure-<component>`
 */
enum class AdventureComponent : Component {
    API,
    NBT,
    KEY,
    BOM,
    ANNOTATION_PROCESSORS;

    override fun getComponent(): String = "adventure-" + name.lowercase().replace("_", "-")

    /**
     * `adventure-platform-<component>`
     */
    enum class Platform : Component {
        API,
        BUKKIT,
        BUNGEECORD,
        SPONGEAPI,
        FABRIC,
        VIAVERSION,
        FACET;

        override fun getComponent(): String = "adventure-platform-" + name.lowercase()
    }

    /**
     * `adventure-extra-<component>`
     */
    enum class Extra : Component {
        KOTLIN;

        override fun getComponent(): String = "adventure-extra-" + name.lowercase()
    }

    /**
     * `adventure-text-<component>`
     */
    enum class Text : Component {
        MINIMESSAGE;

        override fun getComponent(): String = "adventure-text-" + name.lowercase()

        /**
         * `adventure-text-serializer-<component>`
         */
        enum class Serializer : Component {
            LEGACY,
            BUNGEECORD,
            CONFIGURATE3,
            CONFIGURATE4,
            JSON,
            GSON,
            ANSI,
            PLAIN;

            override fun getComponent(): String = "adventure-text-serializer-" + name.lowercase()

            /**
             * `adventure-text-serializer-<component>-impl`
             */
            enum class Implementation : Component {
                JSON,
                GSON;

                override fun getComponent(): String = "adventure-text-serializer-" + name.lowercase() + "-impl"
            }
        }

        /**
         * `adventure-text-logger-<component>`
         */
        enum class Logger : Component {
            SLF4J;

            override fun getComponent(): String = "adventure-text-logger-" + name.lowercase()
        }
    }
}