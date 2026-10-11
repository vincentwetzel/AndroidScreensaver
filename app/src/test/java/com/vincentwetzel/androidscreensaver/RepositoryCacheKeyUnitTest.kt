package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.repository.RepositoryCacheKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RepositoryCacheKeyUnitTest {
    @Test
    fun `cache key components cannot collide across account and folder boundaries`() {
        val firstLegacy = "user_a" + "_" + "b_c" + "_videos_none"
        val secondLegacy = "user_a_b" + "_" + "c" + "_videos_none"
        assertEquals(firstLegacy, secondLegacy)

        assertNotEquals(
            RepositoryCacheKey.of("user_a", "b_c", "videos", RepositoryCacheKey.set(emptySet())),
            RepositoryCacheKey.of("user_a_b", "c", "videos", RepositoryCacheKey.set(emptySet()))
        )
        assertNotEquals(RepositoryCacheKey.of("account", null), RepositoryCacheKey.of("account", "ROOT"))
    }

    @Test
    fun `excluded folder sets remain distinct when IDs contain commas`() {
        assertEquals(setOf("a,b", "c").joinToString(","), setOf("a", "b,c").joinToString(","))
        assertNotEquals(RepositoryCacheKey.set(setOf("a,b", "c")), RepositoryCacheKey.set(setOf("a", "b,c")))
    }
}