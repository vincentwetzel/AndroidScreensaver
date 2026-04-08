package com.vincentwetzel.androidscreensaver.utils

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

/**
 * Utility class for retrieving app version information
 */
object VersionUtils {
    
    /**
     * Get the current app version name
     * @param context Application context
     * @return Version name string (e.g., "1.0.0")
     */
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get the current app version code
     * @param context Application context
     * @return Version code (e.g., 1)
     */
    fun getVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get formatted version string for display
     * @param context Application context
     * @return Formatted version string (e.g., "Version 1.0.0 (build 1)")
     */
    fun getFormattedVersion(context: Context): String {
        val versionName = getVersionName(context)
        val versionCode = getVersionCode(context)
        return "Version $versionName (build $versionCode)"
    }
}