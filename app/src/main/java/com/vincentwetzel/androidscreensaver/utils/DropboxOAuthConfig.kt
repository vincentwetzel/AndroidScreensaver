package com.vincentwetzel.androidscreensaver.utils

/**
 * Dropbox OAuth2 Configuration
 *
 * To update these values:
 * 1. Go to Dropbox Developers Console: https://www.dropbox.com/developers/apps
 * 2. Create or select an app
 * 3. Copy the App key
 */
object DropboxOAuthConfig {

    /**
     * Dropbox App Key
     * From: Dropbox Developers Console > Your App > Settings
     *
     * IMPORTANT: This key should be kept secret and not exposed publicly (e.g., on GitHub).
     * In a production app, consider using build configs (e.g., buildConfigField) or a secure
     * secrets management system.
     */
    const val APP_KEY = "dwz4belhfim678r"
}
