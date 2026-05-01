package com.vincentwetzel.androidscreensaver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vincentwetzel.androidscreensaver.dream.PhotoScreensaverService
import com.vincentwetzel.androidscreensaver.service.ScheduleService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "START_SCREENSAVER" -> {
                val serviceIntent = Intent(context, PhotoScreensaverService::class.java)
                context.startService(serviceIntent)
            }
            "STOP_SCREENSAVER" -> {
                val stopIntent = Intent("com.vincentwetzel.androidscreensaver.STOP_DREAM")
                context.sendBroadcast(stopIntent)
            }
        }
        
        // Reschedule next alarm
        val scheduleIntent = Intent(context, ScheduleService::class.java)
        context.startService(scheduleIntent)
    }
}
