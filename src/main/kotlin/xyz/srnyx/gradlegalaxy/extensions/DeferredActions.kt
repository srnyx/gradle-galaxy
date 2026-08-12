package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project


/**
 * The order in which a deferred action should run, relative to other deferred actions,
 * once the whole `galaxy { }` block has finished executing.
 */
internal enum class Phase {
    /**
     * Setup logic that reads another extension's fields (which may not be configured yet at the point its
     * DSL function runs) and/or mutates a sibling's not-yet-consumed field. Example: `annoyingAPI { }`
     * fetching metadata using the version it was given, and wrapping its own dependency-action to add
     * excludes.
     */
    WIRE,

    /**
     * The terminal step that actually hands something off to Gradle, e.g. [xyz.srnyx.gradlegalaxy.extensions.DependencyExtension.add].
     * Always runs after every [WIRE] action, regardless of the order the two were registered in, so any
     * [WIRE] action that still needs to mutate a dependency's fields is guaranteed to have already done so.
     */
    FINALIZE,
}

/**
 * Collects actions from every `galaxy { }` extension during script evaluation and runs them, ordered by
 * [Phase], from a single [Project.afterEvaluate] callback registered lazily on first use. Everything tagged
 * [Phase.WIRE] runs (in registration order) before anything tagged [Phase.FINALIZE] runs (in registration order),
 * no matter which order the `galaxy { }` block registered them in.
 */
internal class DeferredActions(private val project: Project) {
    private val actions: Map<Phase, MutableList<() -> Unit>> = Phase.entries.associateWith { mutableListOf() }
    private var scheduled = false

    fun defer(phase: Phase, action: () -> Unit) {
        actions.getValue(phase) += action

        if (scheduled) return
        scheduled = true
        project.afterEvaluate {
            Phase.entries.forEach { p -> actions.getValue(p).forEach { it() } }
        }
    }
}
