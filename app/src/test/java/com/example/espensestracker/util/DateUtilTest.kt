package com.example.espensestracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilTest {
    @Test fun pickingTheSameDayKeepsTheTimestamp() {
        val now = System.currentTimeMillis()
        assertEquals(now, mergeDate(now, utcMidnight(now)))
    }
}
