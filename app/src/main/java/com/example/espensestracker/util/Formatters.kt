package com.example.espensestracker.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// TODO Phase 5: make these editable in Settings
const val USER_NAME = "Archit"
const val MONTHLY_BUDGET = 25000.0

fun formatMoney(amount: Double, decimals: Int = 2): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(amount)

fun formatCompact(amount: Double): String =
    if (amount >= 100_000) "₹%.2fL".format(amount / 100_000) else formatMoney(amount, 0)

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))

fun monthLabel(): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun startOfMonthMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis.let { startOfDay(it) }

fun dayLabel(dayStart: Long): String {
    val diff = (startOfDay(System.currentTimeMillis()) - dayStart) / 86_400_000L
    val prefix = when (diff) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(dayStart))
    }
    return "$prefix • " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dayStart))
}
