package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.ui.main.AccountStatusText
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountStatusTextUnitTest {
    @Test
    fun `signed-out remote accounts do not display a stale account email`() {
        assertEquals(
            "Not Connected",
            AccountStatusText.status(false, "person@example.com", "Not Connected")
        )
    }
    @Test
    fun `remote signed-in status names the account email`() {
        assertEquals(
            "Signed in as person@example.com",
            AccountStatusText.signedInAs("person@example.com")
        )
    }
}