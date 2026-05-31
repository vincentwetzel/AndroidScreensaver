package com.vincentwetzel.androidscreensaver.utils

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstract base class for cloud account managers.
 * Centralizes account state tracking and common operations.
 */
abstract class BaseAccountManager<TState>(protected val context: Context) {
    
    protected val accountStates = ConcurrentHashMap<String, TState>()

    /**
     * Check for existing sign-ins on device and restore known accounts.
     */
    abstract fun checkExistingSignIn()

    /**
     * Get the access token for a specific account.
     */
    abstract fun getAccessToken(accountId: String): String?

    /**
     * Check if a specific account is currently authenticated.
     */
    open fun isAccountAuthenticated(accountId: String): Boolean {
        return accountStates.containsKey(accountId)
    }

    /**
     * Get all currently authenticated account IDs.
     */
    fun getAuthenticatedAccountIds(): Set<String> {
        return accountStates.keys.toSet()
    }

    /**
     * Sign out and remove a specific account.
     */
    open fun signOutAccount(accountId: String) {
        accountStates.remove(accountId)
    }

    /**
     * Sign out all accounts locally.
     */
    open fun signOutAll() {
        accountStates.clear()
    }

    /**
     * Revoke remote access and remove all accounts.
     */
    abstract suspend fun revokeAll()
}