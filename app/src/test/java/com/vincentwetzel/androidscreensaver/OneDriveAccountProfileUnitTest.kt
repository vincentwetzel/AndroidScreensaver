package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.auth.OneDriveAccountProfilePolicy
import com.vincentwetzel.androidscreensaver.auth.OneDriveTokenPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OneDriveAccountProfileUnitTest {

    @Test
    fun `Graph mail and stable id are used for account routing`() {
        val account = OneDriveAccountProfilePolicy.fromFields(
            graphId = "graph-user-id",
            mail = "person@example.com",
            userPrincipalName = "person@tenant.onmicrosoft.com",
            fallbackEmail = "fallback@example.com"
        )

        assertEquals("graph-user-id", account?.accountId)
        assertEquals("person@example.com", account?.email)
    }

    @Test
    fun `user principal name is used when Graph mail is absent`() {
        val account = OneDriveAccountProfilePolicy.fromFields(
            graphId = "graph-user-id",
            mail = null,
            userPrincipalName = "person@tenant.onmicrosoft.com",
            fallbackEmail = null
        )

        assertEquals("person@tenant.onmicrosoft.com", account?.email)
    }

    @Test
    fun `profile without email is rejected unless MSAL supplies an email fallback`() {
        assertNull(OneDriveAccountProfilePolicy.fromFields("id", null, null, null))
        assertEquals(
            "person@example.com",
            OneDriveAccountProfilePolicy.fromFields("id", null, null, "person@example.com")?.email
        )
    }

    @Test
    fun `tokens refresh before expiry and expired tokens are not used after refresh failure`() {
        val now = 10_000L

        assertFalse(OneDriveTokenPolicy.shouldRefresh(now + 120_000L, now))
        assertTrue(OneDriveTokenPolicy.shouldRefresh(now + 30_000L, now))
        assertTrue(OneDriveTokenPolicy.canUseAfterRefreshFailure(now + 1L, now))
        assertFalse(OneDriveTokenPolicy.canUseAfterRefreshFailure(now, now))
    }
}
