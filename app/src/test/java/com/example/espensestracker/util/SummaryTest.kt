package com.example.espensestracker.util

import com.example.espensestracker.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryTest {
    @Test fun forecastIsStraightLine() {
        assertEquals(3000.0, forecastMonthEnd(1000.0, 10, 30), 0.001)
        assertEquals(500.0, forecastMonthEnd(500.0, 0, 30), 0.001)
    }

    @Test fun weekSummaryComparesWithPreviousWeek() {
        val now = 1_700_000_000_000L
        val d = 86_400_000L
        val list = listOf(
            Transaction(amount = 100.0, category = "Food & Dining", timestamp = now - d),
            Transaction(amount = 300.0, category = "Rides & Travel", timestamp = now - 2 * d),
            Transaction(amount = 50.0, category = "Other", timestamp = now - 8 * d)
        )
        val s = weekSummary(list, now, false)
        assertEquals(400.0, s.spent, 0.001)
        assertEquals(50.0, s.prevSpent, 0.001)
        assertEquals("Rides & Travel", s.topCategory)
        assertEquals(2, s.count)
    }
}
