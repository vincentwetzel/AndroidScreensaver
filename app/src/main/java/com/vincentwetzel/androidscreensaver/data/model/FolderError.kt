package com.vincentwetzel.androidscreensaver.data.model

/**
 * Sealed class representing different types of folder loading errors
 * with user-friendly messages
 */
sealed class FolderError {
    data class NetworkError(val message: String = "Unable to connect. Please check your internet connection and try again.") : FolderError()
    data class AuthError(val message: String = "Authentication expired. Please sign in again.") : FolderError()
    data class PermissionError(val message: String = "Permission denied. Please grant storage access in Settings.") : FolderError()
    data class ApiError(val message: String = "Unable to load folders. Please try again later.") : FolderError()
    data class EmptyError(val message: String = "No folders found. Make sure you have photos in your account.") : FolderError()
    data class UnknownError(val message: String = "An unexpected error occurred. Please try again.") : FolderError()

    /**
     * Convert an exception to the appropriate FolderError
     */
    companion object {
        fun fromException(e: Exception): FolderError {
            val message = e.message?.lowercase() ?: ""
            return when {
                // Network errors
                message.contains("network") ||
                message.contains("unable to resolve") ||
                message.contains("connection") ||
                message.contains("timeout") ||
                message.contains("socket") -> NetworkError()

                // Auth errors
                message.contains("auth") ||
                message.contains("unauthorized") ||
                message.contains("401") ||
                message.contains("sign") ||
                message.contains("token") ||
                message.contains("credential") -> AuthError()

                // Permission errors
                message.contains("permission") ||
                message.contains("denied") ||
                message.contains("access") -> PermissionError()

                // API errors
                message.contains("api") ||
                message.contains("server") ||
                message.contains("500") ||
                message.contains("502") ||
                message.contains("503") -> ApiError()

                // Empty result
                message.contains("empty") ||
                message.contains("no ") -> EmptyError()

                else -> UnknownError(e.message ?: "An unexpected error occurred.")
            }
        }

        /**
         * Get the user-friendly message for this error
         */
        fun FolderError.userMessage(): String = when (this) {
            is NetworkError -> message
            is AuthError -> message
            is PermissionError -> message
            is ApiError -> message
            is EmptyError -> message
            is UnknownError -> message
        }
    }
}
