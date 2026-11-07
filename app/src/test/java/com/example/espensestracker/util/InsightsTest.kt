package com.example.espensestracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsTest {
    @Test fun currentMonthContainsNow() {
        val m = monthRange(0)
        assertTrue(System.currentTimeMillis() in m.start..m.end)
        assertTrue(m.days in 28..31)
    }

    @Test fun monthsAreContiguous() {
        assertEquals(monthRange(0).start, monthRange(-1).end + 1)
        assertEquals(monthRange(-1).start, monthRange(-2).end + 1)
    }
}
