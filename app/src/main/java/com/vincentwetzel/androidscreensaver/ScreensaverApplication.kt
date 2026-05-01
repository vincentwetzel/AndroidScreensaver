package com.vincentwetzel.androidscreensaver

import android.app.Application
import android.os.Build
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class ScreensaverApplication : Application() {

    companion object {
        private const val TAG = "ScreensaverApplication"
        private const val LOG_FILE_NAME = "debug-logcat.txt"
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG_LOGCAT_MIRROR) {
            startLogcatMirror()
        }
    }

    /**
     * Starts a background logcat process that mirrors all log output to a file.
     * Normal logcat output continues to work as usual - this just creates a copy.
     */
    private fun startLogcatMirror() {
        try {
            // Use app-specific external files directory (auto-cleaned on uninstall)
            val logFile = File(getExternalFilesDir(null), LOG_FILE_NAME)
            
            // Clear existing log to start fresh
            Runtime.getRuntime().exec("logcat -c")
            
            // Start logcat in background, appending to file
            // -v threadtime: verbose format with thread info
            // *:V: capture all priority levels
            val process = Runtime.getRuntime().exec("logcat -v threadtime *:V")
            
            // Read logcat output and write to file
            Thread {
                try {
                    process.inputStream.bufferedReader().use { reader ->
                        logFile.bufferedWriter().use { writer ->
                            writer.appendLine("=== Logcat Mirror Started ===")
                            writer.appendLine("Device: ${Build.MODEL}")
                            writer.appendLine("App Version: ${Build.VERSION.SDK_INT}")
                            writer.appendLine("===========================")
                            
                            reader.forEachLine { line ->
                                writer.appendLine(line)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading logcat stream", e)
                }
            }.apply {
                isDaemon = true
                name = "logcat-mirror"
                start()
            }
            
            Log.i(TAG, "Logcat mirroring started. Log file: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start logcat mirroring", e)
        }
    }
}
