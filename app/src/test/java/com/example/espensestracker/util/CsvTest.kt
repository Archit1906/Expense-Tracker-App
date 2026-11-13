package com.example.espensestracker.util

import com.example.espensestracker.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTest {
    @Test fun escapesCommasAndQuotes() {
        val csv = toCsv(listOf(Transaction(amount = 1240.0, merchant = "Swiggy, Inc", note = "He said \"hi\"")))
        assertTrue(csv.contains("\"Swiggy, Inc\""))
        assertTrue(csv.contains("\"He said \"\"hi\"\"\""))
        assertTrue(csv.contains("1240.00"))
        assertEquals(2, csv.trim().lines().size)
    }
}
