package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.repository.requireOneDrivePageBody
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OneDrivePageResponseUnitTest {

    @Test
    fun `successful Graph response requires and returns a body`() {
        assertEquals("{\"value\":[]}", requireOneDrivePageBody(200, "{\"value\":[]}"))
    }

    @Test
    fun `failed Graph response throws instead of allowing a partial listing`() {
        assertThrows(IOException::class.java) {
            requireOneDrivePageBody(503, "temporary error")
        }
    }

    @Test
    fun `empty Graph response body throws instead of allowing an empty listing`() {
        assertThrows(IOException::class.java) {
            requireOneDrivePageBody(200, null)
        }
    }
}
