package com.vincentwetzel.androidscreensaver.utils

/**
 * Google OAuth2 Configuration
 * 
 * To update these values:
 * 1. Go to Google Cloud Console: https://console.cloud.google.com/apis/credentials
 * 2. Find your OAuth 2.0 Client ID
 * 3. Copy the Client ID value
 */
object GoogleOAuthConfig {

    /**
     * OAuth 2.0 Client ID for Android
     * From: Google Cloud Console > APIs & Services > Credentials
     * Type: Android
     */
    val CLIENT_ID: String = "" // TODO: Replace with BuildConfig.GOOGLE_CLIENT_ID once buildConfigField is added to build.gradle.kts

    /**
     * OAuth 2.0 Web Client ID (for ID token requests)
     * From: Google Cloud Console > APIs & Services > Credentials
     * Type: Web application
     *
     * TODO: Create a "Web application" OAuth client in Google Cloud Console
     * and replace this with its Client ID.
     * If you don't need ID tokens, this can be left empty.
     */
    const val WEB_CLIENT_ID = ""
    
    /**
     * Required scope for Google Drive read-only access
     */
    const val DRIVE_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
    
    /**
     * Required scope for Google Drive metadata read-only
     */
    const val DRIVE_METADATA_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.metadata.readonly"
    
    /**
     * All required scopes for Google Drive integration
     */
    val SCOPES = listOf(
        DRIVE_READONLY_SCOPE,
        DRIVE_METADATA_READONLY_SCOPE
    )
    
    /**
     * Project ID from Google Cloud Console
     */
    const val PROJECT_ID = "androidscreensaver"
}
