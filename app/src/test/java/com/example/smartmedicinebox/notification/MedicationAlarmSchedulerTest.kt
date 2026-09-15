package com.example.smartmedicinebox.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MedicationAlarmSchedulerTest {
    private val zone = ZoneId.of("Asia/Kuala_Lumpur")

    @Test
    fun nextReminderTime_usesTodayWhenTimeIsStillAhead() {
        val now = ZonedDateTime.of(2026, 9, 15, 8, 30, 20, 0, zone)

        val result = nextReminderTime("09:15", now)

        assertEquals(ZonedDateTime.of(2026, 9, 15, 9, 15, 0, 0, zone), result)
    }

    @Test
    fun nextReminderTime_usesTomorrowWhenTimeHasPassed() {
        val now = ZonedDateTime.of(2026, 9, 15, 23, 59, 0, 0, zone)

        val result = nextReminderTime("00:05", now)

        assertEquals(ZonedDateTime.of(2026, 9, 16, 0, 5, 0, 0, zone), result)
    }

    @Test
    fun nextReminderTime_doesNotRepeatWithinTheSameMinute() {
        val now = ZonedDateTime.of(2026, 9, 15, 9, 15, 1, 0, zone)

        val result = nextReminderTime("09:15", now)

        assertEquals(ZonedDateTime.of(2026, 9, 16, 9, 15, 0, 0, zone), result)
    }
}
