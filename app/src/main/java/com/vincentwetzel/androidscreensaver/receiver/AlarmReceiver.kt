package com.vincentwetzel.androidscreensaver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.vincentwetzel.androidscreensaver.dream.PhotoScreensaverService
import com.vincentwetzel.androidscreensaver.service.ScheduleService
import com.vincentwetzel.androidscreensaver.utils.ScheduleAlarmPolicy
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SCHEDULE_INDEX = "schedule_index"
        const val EXTRA_IS_AUTOSTART = "schedule_is_autostart"
        const val EXTRA_REPEAT = "schedule_repeat"
        const val EXTRA_ONLY_WHEN_CHARGING = "schedule_only_when_charging"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isAutostart = intent.getBooleanExtra(EXTRA_IS_AUTOSTART, intent.action == "START_SCREENSAVER")
        val shouldRun = when (intent.action) {
            "START_SCREENSAVER" -> ScheduleAlarmPolicy.shouldRunAutostart(
                intent.getBooleanExtra(EXTRA_ONLY_WHEN_CHARGING, false),
                isDeviceCharging(context)
            )
            "STOP_SCREENSAVER" -> true
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (shouldRun) {
                    when (intent.action) {
                        "START_SCREENSAVER" -> context.startService(Intent(context, PhotoScreensaverService::class.java))
                        "STOP_SCREENSAVER" -> {
                            val stopIntent = Intent("com.vincentwetzel.androidscreensaver.STOP_DREAM")
                                .setPackage(context.packageName)
                            context.sendBroadcast(stopIntent)
                        }
                    }

                    val isRepeating = intent.getBooleanExtra(EXTRA_REPEAT, true)
                    val scheduleIndex = intent.getIntExtra(EXTRA_SCHEDULE_INDEX, -1)
                    if (ScheduleAlarmPolicy.shouldDisableAfterTrigger(isRepeating, actionExecuted = true) && scheduleIndex >= 0) {
                        SettingsManager.disableOneShotSchedule(context, isAutostart, scheduleIndex)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmReceiver", "Failed to execute scheduled action: ${e.javaClass.simpleName}")
            } finally {
                try {
                    // Recompute the next alarm, including after a charging-only alarm is skipped.
                    context.startService(Intent(context, ScheduleService::class.java))
                } catch (e: Exception) {
                    android.util.Log.e("AlarmReceiver", "Failed to reschedule alarms: ${e.javaClass.simpleName}")
                }
                pendingResult.finish()
            }
        }
    }

    private fun isDeviceCharging(context: Context): Boolean {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            batteryStatus == BatteryManager.BATTERY_STATUS_FULL
    }
}
