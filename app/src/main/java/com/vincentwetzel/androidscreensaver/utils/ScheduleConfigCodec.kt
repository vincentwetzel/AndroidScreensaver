package com.vincentwetzel.androidscreensaver.utils

import com.vincentwetzel.androidscreensaver.data.model.DayOfWeek
import com.vincentwetzel.androidscreensaver.data.model.ScheduleConfig
import com.vincentwetzel.androidscreensaver.data.model.SchedulePreset

/** Pure delimiter-safe codec for schedule settings stored in a single DataStore value. */
internal object ScheduleConfigCodec {

    fun decode(serialized: String): List<ScheduleConfig> {
        if (serialized.isEmpty()) return emptyList()

        return serialized.split(";;").filter(String::isNotEmpty).mapNotNull { record ->
            val parts = record.split("|")
            if (parts.size != 8) return@mapNotNull null

            val hour = parts[1].toIntOrNull()?.takeIf { it in 0..23 } ?: return@mapNotNull null
            val minute = parts[2].toIntOrNull()?.takeIf { it in 0..59 } ?: return@mapNotNull null
            val preset = runCatching { SchedulePreset.valueOf(parts[4]) }.getOrNull()
                ?: return@mapNotNull null
            val days = if (parts[3].isEmpty()) {
                emptySet()
            } else {
                parts[3].split(",").mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
            }

            ScheduleConfig(
                enabled = parts[0].toBooleanStrictOrNull() ?: return@mapNotNull null,
                timeHour = hour,
                timeMinute = minute,
                daysOfWeek = days,
                schedulePreset = preset,
                repeat = parts[5].toBooleanStrictOrNull() ?: return@mapNotNull null,
                onlyWhenCharging = parts[6].toBooleanStrictOrNull() ?: return@mapNotNull null,
                syncWithAutostartDays = parts[7].toBooleanStrictOrNull() ?: return@mapNotNull null
            )
        }
    }

    fun replaceFirst(schedules: List<ScheduleConfig>, updated: ScheduleConfig): List<ScheduleConfig> =
        if (schedules.isEmpty()) listOf(updated) else schedules.toMutableList().apply { this[0] = updated }

    fun encode(schedules: List<ScheduleConfig>): String = schedules.joinToString(";;") { schedule ->
        val days = schedule.daysOfWeek.sortedBy(DayOfWeek::ordinal).joinToString(",") { it.name }
        "${schedule.enabled}|${schedule.timeHour}|${schedule.timeMinute}|$days|${schedule.schedulePreset.name}|" +
            "${schedule.repeat}|${schedule.onlyWhenCharging}|${schedule.syncWithAutostartDays}"
    }
}
