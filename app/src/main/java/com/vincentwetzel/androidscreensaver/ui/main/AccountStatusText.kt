package com.vincentwetzel.androidscreensaver.ui.main

internal object AccountStatusText {
    fun signedInAs(accountEmail: String): String = "Signed in as $accountEmail"

    fun status(isAuthenticated: Boolean, accountEmail: String, signedOutText: String): String =
        if (isAuthenticated) signedInAs(accountEmail) else signedOutText
}