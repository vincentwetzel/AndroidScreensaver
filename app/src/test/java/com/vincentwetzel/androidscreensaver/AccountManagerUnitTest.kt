package com.vincentwetzel.androidscreensaver

import android.content.ContextWrapper
import com.vincentwetzel.androidscreensaver.utils.BaseAccountManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountManagerUnitTest {

    @Test
    fun `account manager tracks authentication and removes one account`() = runBlocking {
        val manager = FakeAccountManager()
        manager.authenticate("one", "token-one")
        manager.authenticate("two", "token-two")

        assertEquals(setOf("one", "two"), manager.getAuthenticatedAccountIds())
        assertTrue(manager.isAccountAuthenticated("one"))
        assertEquals("token-two", manager.getAccessToken("two"))

        manager.signOutAccount("one")

        assertEquals(setOf("two"), manager.getAuthenticatedAccountIds())
        assertFalse(manager.isAccountAuthenticated("one"))
        assertEquals(null, manager.getAccessToken("one"))
    }

    @Test
    fun `sign out all and revoke all clear account state`() = runBlocking {
        val manager = FakeAccountManager()
        manager.authenticate("one", "token-one")
        manager.authenticate("two", "token-two")

        manager.signOutAll()
        assertTrue(manager.getAuthenticatedAccountIds().isEmpty())

        manager.authenticate("three", "token-three")
        manager.revokeAll()
        assertTrue(manager.getAuthenticatedAccountIds().isEmpty())
    }

    private class FakeAccountManager : BaseAccountManager<String>(ContextWrapper(null)) {
        fun authenticate(accountId: String, accessToken: String) {
            accountStates[accountId] = accessToken
        }

        override fun checkExistingSignIn() = Unit

        override fun getAccessToken(accountId: String): String? = accountStates[accountId]

        override suspend fun revokeAll() {
            signOutAll()
        }
    }
}
