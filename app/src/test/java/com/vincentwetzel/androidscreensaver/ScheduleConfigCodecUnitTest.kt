package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.data.model.DayOfWeek
import com.vincentwetzel.androidscreensaver.data.model.ScheduleConfig
import com.vincentwetzel.androidscreensaver.data.model.SchedulePreset
import com.vincentwetzel.androidscreensaver.utils.ScheduleConfigCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleConfigCodecUnitTest {

    @Test
    fun `schedule codec round trips all schedule behavior fields`() {
        val schedules = listOf(
            ScheduleConfig(
                enabled = true,
                timeHour = 6,
                timeMinute = 45,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                schedulePreset = SchedulePreset.CUSTOM,
                repeat = false,
                onlyWhenCharging = true,
                syncWithAutostartDays = true
            ),
            ScheduleConfig(enabled = false, daysOfWeek = emptySet())
        )

        assertEquals(schedules, ScheduleConfigCodec.decode(ScheduleConfigCodec.encode(schedules)))
    }

    @Test
    fun `editing first schedule preserves later schedules`() {
        val first = ScheduleConfig(enabled = true, timeHour = 6, timeMinute = 30)
        val later = ScheduleConfig(enabled = false, timeHour = 22, timeMinute = 15)
        val updated = first.copy(timeHour = 7)

        assertEquals(listOf(updated, later), ScheduleConfigCodec.replaceFirst(listOf(first, later), updated))
        assertEquals(listOf(updated), ScheduleConfigCodec.replaceFirst(emptyList(), updated))
    }
    @Test
    fun `empty and malformed schedule records decode safely`() {
        assertTrue(ScheduleConfigCodec.decode("").isEmpty())
        assertTrue(ScheduleConfigCodec.decode("broken|record").isEmpty())
    }
}
