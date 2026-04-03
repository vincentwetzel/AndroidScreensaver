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
     */
    const val CLIENT_ID = "459442467476-r9unkjslnp6giff3v0iv642hiss1ntap.apps.googleusercontent.com"
    
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
