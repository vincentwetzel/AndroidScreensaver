package com.vincentwetzel.androidscreensaver

import com.vincentwetzel.androidscreensaver.utils.ScheduleAlarmPolicy
import com.vincentwetzel.androidscreensaver.service.shouldUseExactScheduleAlarm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleAlarmPolicyUnitTest {

    @Test
    fun `exact alarm policy respects platform permission availability`() {
        assertTrue(shouldUseExactScheduleAlarm(sdkInt = 30, exactAlarmAccessGranted = false))
        assertFalse(shouldUseExactScheduleAlarm(sdkInt = 31, exactAlarmAccessGranted = false))
        assertTrue(shouldUseExactScheduleAlarm(sdkInt = 31, exactAlarmAccessGranted = true))
    }

    @Test
    fun `charging requirement only blocks autostart while unplugged`() {
        assertTrue(ScheduleAlarmPolicy.shouldRunAutostart(onlyWhenCharging = false, deviceCharging = false))
        assertTrue(ScheduleAlarmPolicy.shouldRunAutostart(onlyWhenCharging = true, deviceCharging = true))
        assertFalse(ScheduleAlarmPolicy.shouldRunAutostart(onlyWhenCharging = true, deviceCharging = false))
    }

    @Test
    fun `one shot is disabled only after its action runs`() {
        assertTrue(ScheduleAlarmPolicy.shouldDisableAfterTrigger(repeating = false, actionExecuted = true))
        assertFalse(ScheduleAlarmPolicy.shouldDisableAfterTrigger(repeating = true, actionExecuted = true))
        assertFalse(ScheduleAlarmPolicy.shouldDisableAfterTrigger(repeating = false, actionExecuted = false))
    }
}
