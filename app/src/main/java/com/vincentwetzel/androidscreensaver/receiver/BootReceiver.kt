package com.vincentwetzel.androidscreensaver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vincentwetzel.androidscreensaver.service.ScheduleService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val serviceIntent = Intent(context, ScheduleService::class.java)
            context.startService(serviceIntent)
        }
    }
}
