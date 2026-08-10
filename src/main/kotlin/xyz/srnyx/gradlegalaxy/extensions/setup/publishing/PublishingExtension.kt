package xyz.srnyx.gradlegalaxy.extensions.setup.publishing

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingEnvExtension
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingPlatformExtension
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingSimpleExtension
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import javax.inject.Inject


abstract class PublishingExtension @Inject constructor(
    objects: ObjectFactory
) {
    val simple = objects.newInstance(PublishingSimpleExtension::class.java)
    val env = objects.newInstance(PublishingEnvExtension::class.java)
    val platforms = objects.newInstance(PublishingPlatformExtension::class.java)

    fun simple(action: PublishingSimpleExtension.() -> Unit) {
        simple.configured = true
        simple.action()
    }
    fun env(action: PublishingEnvExtension.() -> Unit) {
        env.configured = true
        env.action()
    }
    fun platforms(platforms: Map<PluginPlatform, String>, action: PublishingPlatformExtension.() -> Unit) {
        this.platforms.configured = true
        this.platforms.platforms = platforms
        this.platforms.action()
    }

    fun process(project: Project) {
        if (simple.configured) simple.setup(project)
        if (env.configured) env.setup(project)
        if (platforms.configured) platforms.setup(project)
    }
}
