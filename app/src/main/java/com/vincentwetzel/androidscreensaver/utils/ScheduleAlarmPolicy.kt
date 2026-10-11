package com.vincentwetzel.androidscreensaver.utils

/** Policy shared by alarm execution and focused JVM tests. */
internal object ScheduleAlarmPolicy {
    fun shouldRunAutostart(onlyWhenCharging: Boolean, deviceCharging: Boolean): Boolean =
        !onlyWhenCharging || deviceCharging

    fun shouldDisableAfterTrigger(repeating: Boolean, actionExecuted: Boolean): Boolean =
        actionExecuted && !repeating
}
