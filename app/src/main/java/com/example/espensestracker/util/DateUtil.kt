package com.example.espensestracker.util

import java.util.Calendar
import java.util.TimeZone

/** The Material date picker works in UTC midnight; these convert to/from local timestamps. */
fun utcMidnight(localMillis: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Takes the picked calendar date, keeps the time of day of [currentLocal]. */
fun mergeDate(currentLocal: Long, pickedUtc: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedUtc }
    return Calendar.getInstance().apply {
        timeInMillis = currentLocal
        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
