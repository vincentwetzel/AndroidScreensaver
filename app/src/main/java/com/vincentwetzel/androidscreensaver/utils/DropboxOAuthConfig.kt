package com.vincentwetzel.androidscreensaver.utils

import com.vincentwetzel.androidscreensaver.BuildConfig

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
     * Injected via BuildConfig from local.properties to follow Secret Management standards.
     */
    val APP_KEY: String = BuildConfig.DROPBOX_APP_KEY
}
