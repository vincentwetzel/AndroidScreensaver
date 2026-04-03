package com.vincentwetzel.androidscreensaver.utils

/**
 * Obfuscated URLs to prevent scraper detection.
 * These are reconstructed at runtime.
 */
object SecureLinks {
    
    /**
     * Discord server invite link
     * Obfuscated to prevent scraper detection
     */
    val discordInvite: String
        get() {
            val parts = listOf(
                "https",
                "://",
                "discord",
                ".gg/",
                "wWhudUEqt6"
            )
            return parts.joinToString("")
        }
    
    /**
     * GitHub repository URL
     */
    val githubRepo: String
        get() {
            val parts = listOf(
                "https://github",
                ".com/",
                "vincentwetzel/",
                "AndroidScreensaver"
            )
            return parts.joinToString("")
        }
}
