package com.example.espensestracker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthRange(val start: Long, val end: Long, val label: String, val days: Int)

/** offset 0 = this month, -1 = last month, ... */
fun monthRange(offset: Int): MonthRange {
    val c = Calendar.getInstance().apply { add(Calendar.MONTH, offset); set(Calendar.DAY_OF_MONTH, 1) }
    val days = c.getActualMaximum(Calendar.DAY_OF_MONTH)
    val start = startOfDay(c.timeInMillis)
    val end = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, 1) }.timeInMillis - 1
    return MonthRange(start, end, SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(start)), days)
}

fun dayOfMonth(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)
