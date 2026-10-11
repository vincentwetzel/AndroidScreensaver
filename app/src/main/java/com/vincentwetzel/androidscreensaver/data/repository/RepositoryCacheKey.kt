package com.vincentwetzel.androidscreensaver.data.repository

/** Encodes repository cache key components without delimiter collisions. */
internal object RepositoryCacheKey {
    fun of(vararg components: String?): String = buildString {
        components.forEach { component ->
            if (component == null) {
                append("-1:")
            } else {
                append(component.length).append(':').append(component)
            }
        }
    }

    fun set(values: Set<String>): String = buildString {
        append(values.size).append(':')
        values.sorted().forEach { value -> append(value.length).append(':').append(value) }
    }
}