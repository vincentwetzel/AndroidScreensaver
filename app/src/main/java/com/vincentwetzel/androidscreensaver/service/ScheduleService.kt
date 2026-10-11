package com.vincentwetzel.androidscreensaver.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.os.Build
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.vincentwetzel.androidscreensaver.data.model.DayOfWeek
import com.vincentwetzel.androidscreensaver.receiver.AlarmReceiver
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScheduleService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scheduleJob: Job? = null

    companion object {
        private const val TAG = "ScheduleService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleJob?.cancel()
        scheduleJob = serviceScope.launch {
            try {
                val config = SettingsManager.getSlideshowConfig(this@ScheduleService)

                config.autostartSchedules.forEachIndexed { index, schedule ->
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val alarmIntent = Intent(this@ScheduleService, AlarmReceiver::class.java).let { alarm ->
                        alarm.action = "START_SCREENSAVER"
                        alarm.putExtra(AlarmReceiver.EXTRA_SCHEDULE_INDEX, index)
                        alarm.putExtra(AlarmReceiver.EXTRA_IS_AUTOSTART, true)
                        alarm.putExtra(AlarmReceiver.EXTRA_REPEAT, schedule.repeat)
                        alarm.putExtra(AlarmReceiver.EXTRA_ONLY_WHEN_CHARGING, schedule.onlyWhenCharging)
                        PendingIntent.getBroadcast(
                            this@ScheduleService,
                            index,
                            alarm,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                    if (schedule.enabled) {
                        getNextAlarmTime(schedule.timeHour, schedule.timeMinute, schedule.daysOfWeek)
                            ?.let { scheduleAlarm(alarmManager, it.timeInMillis, alarmIntent) }
                    } else {
                        alarmManager.cancel(alarmIntent)
                    }
                }

                config.autostopSchedules.forEachIndexed { index, schedule ->
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val alarmIntent = Intent(this@ScheduleService, AlarmReceiver::class.java).let { alarm ->
                        alarm.action = "STOP_SCREENSAVER"
                        alarm.putExtra(AlarmReceiver.EXTRA_SCHEDULE_INDEX, index)
                        alarm.putExtra(AlarmReceiver.EXTRA_IS_AUTOSTART, false)
                        alarm.putExtra(AlarmReceiver.EXTRA_REPEAT, schedule.repeat)
                        PendingIntent.getBroadcast(
                            this@ScheduleService,
                            100 + index,
                            alarm,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                    if (schedule.enabled) {
                        getNextAlarmTime(schedule.timeHour, schedule.timeMinute, schedule.daysOfWeek)
                            ?.let { scheduleAlarm(alarmManager, it.timeInMillis, alarmIntent) }
                    } else {
                        alarmManager.cancel(alarmIntent)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to configure schedules: ${e.javaClass.simpleName}")
            } finally {
                stopSelfResult(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scheduleJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
    @SuppressLint("MissingPermission") // Exact access is checked before setExact; denied devices use the inexact fallback.
    private fun scheduleAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, intent: PendingIntent) {
        val exactAlarmAccessGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (shouldUseExactScheduleAlarm(Build.VERSION.SDK_INT, exactAlarmAccessGranted)) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        } else {
            // Keep schedules functional when Android has not granted exact-alarm access.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
            android.util.Log.w(TAG, "Exact alarm access unavailable; scheduled an inexact fallback")
        }
    }
    private fun getNextAlarmTime(hour: Int, minute: Int, days: Set<DayOfWeek>): Calendar? {
        if (days.isEmpty()) return null

        val sortedDays = days.map { convertDayOfWeek(it) }.sorted()
        
        val now = Calendar.getInstance()
        
        for (day in sortedDays) {
            val next = now.clone() as Calendar
            next.set(Calendar.DAY_OF_WEEK, day)
            next.set(Calendar.HOUR_OF_DAY, hour)
            next.set(Calendar.MINUTE, minute)
            next.set(Calendar.SECOND, 0)
            next.set(Calendar.MILLISECOND, 0)
            
            if (next.after(now)) {
                return next
            }
        }
        
        val firstDay = sortedDays.first()
        val next = now.clone() as Calendar
        next.add(Calendar.WEEK_OF_YEAR, 1)
        next.set(Calendar.DAY_OF_WEEK, firstDay)
        next.set(Calendar.HOUR_OF_DAY, hour)
        next.set(Calendar.MINUTE, minute)
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        
        return next
    }
    
    private fun convertDayOfWeek(day: DayOfWeek): Int {
        return when(day) {
            DayOfWeek.SUNDAY -> Calendar.SUNDAY
            DayOfWeek.MONDAY -> Calendar.MONDAY
            DayOfWeek.TUESDAY -> Calendar.TUESDAY
            DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            DayOfWeek.THURSDAY -> Calendar.THURSDAY
            DayOfWeek.FRIDAY -> Calendar.FRIDAY
            DayOfWeek.SATURDAY -> Calendar.SATURDAY
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

internal fun shouldUseExactScheduleAlarm(sdkInt: Int, exactAlarmAccessGranted: Boolean): Boolean =
    sdkInt < Build.VERSION_CODES.S || exactAlarmAccessGranted
