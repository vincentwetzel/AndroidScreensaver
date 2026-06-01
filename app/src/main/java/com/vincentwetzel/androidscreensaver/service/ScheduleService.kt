package com.vincentwetzel.androidscreensaver.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.vincentwetzel.androidscreensaver.data.model.DayOfWeek
import com.vincentwetzel.androidscreensaver.receiver.AlarmReceiver
import com.vincentwetzel.androidscreensaver.utils.SettingsManager
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            val config = SettingsManager.getSlideshowConfig(this@ScheduleService)
            
            config.autostartSchedules.forEachIndexed { index, schedule ->
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(this@ScheduleService, AlarmReceiver::class.java).let { intent ->
                    intent.action = "START_SCREENSAVER"
                    PendingIntent.getBroadcast(this@ScheduleService, index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                
                if (schedule.enabled) {
                    val nextAlarmTime = getNextAlarmTime(schedule.timeHour, schedule.timeMinute, schedule.daysOfWeek)
                    if (nextAlarmTime != null) {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime.timeInMillis,
                            alarmIntent
                        )
                    }
                } else {
                    alarmManager.cancel(alarmIntent)
                }
            }
            
            config.autostopSchedules.forEachIndexed { index, schedule ->
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(this@ScheduleService, AlarmReceiver::class.java).let { intent ->
                    intent.action = "STOP_SCREENSAVER"
                    PendingIntent.getBroadcast(this@ScheduleService, 100 + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                if (schedule.enabled) {
                    val nextAlarmTime = getNextAlarmTime(schedule.timeHour, schedule.timeMinute, schedule.daysOfWeek)
                    if (nextAlarmTime != null) {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime.timeInMillis,
                            alarmIntent
                        )
                    }
                } else {
                    alarmManager.cancel(alarmIntent)
                }
            }
        }

        return START_NOT_STICKY
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
