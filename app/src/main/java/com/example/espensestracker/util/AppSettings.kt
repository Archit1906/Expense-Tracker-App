package com.example.espensestracker.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User settings, backed by SharedPreferences and observable from Compose. */
object AppSettings {
    private const val FILE = "app"

    var budget by mutableDoubleStateOf(MONTHLY_BUDGET)
        private set
    var lockEnabled by mutableStateOf(false)
        private set
    var netCredits by mutableStateOf(true)
        private set
    var themeMode by mutableIntStateOf(0)           // 0 system, 1 light, 2 dark
        private set
    var alertsEnabled by mutableStateOf(false)      // unusual-spend notifications
        private set
    var weeklyEnabled by mutableStateOf(false)      // weekly summary notification
        private set

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        val p = prefs(ctx)
        budget = p.getLong("budget", MONTHLY_BUDGET.toLong()).toDouble()
        lockEnabled = p.getBoolean("lock", false)
        netCredits = p.getBoolean("net_credits", true)
        themeMode = p.getInt("theme", 0)
        alertsEnabled = p.getBoolean("alerts", false)
        weeklyEnabled = p.getBoolean("weekly", false)
    }

    fun saveBudget(ctx: Context, value: Double) {
        budget = value
        prefs(ctx).edit().putLong("budget", value.toLong()).apply()
    }

    fun saveNetCredits(ctx: Context, on: Boolean) {
        netCredits = on
        prefs(ctx).edit().putBoolean("net_credits", on).apply()
    }

    fun saveLock(ctx: Context, on: Boolean) {
        lockEnabled = on
        prefs(ctx).edit().putBoolean("lock", on).apply()
    }

    fun saveTheme(ctx: Context, mode: Int) {
        themeMode = mode
        prefs(ctx).edit().putInt("theme", mode).apply()
    }

    fun saveAlerts(ctx: Context, on: Boolean) {
        alertsEnabled = on
        prefs(ctx).edit().putBoolean("alerts", on).apply()
    }

    fun saveWeekly(ctx: Context, on: Boolean) {
        weeklyEnabled = on
        prefs(ctx).edit().putBoolean("weekly", on).apply()
    }
}
