package com.example.espensestracker.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User settings, backed by SharedPreferences and observable from Compose. */
object AppSettings {
    private const val FILE = "app"

    var budget by mutableDoubleStateOf(MONTHLY_BUDGET)
        private set
    var lockEnabled by mutableStateOf(false)
        private set

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        budget = p.getLong("budget", MONTHLY_BUDGET.toLong()).toDouble()
        lockEnabled = p.getBoolean("lock", false)
    }

    fun saveBudget(ctx: Context, value: Double) {
        budget = value
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putLong("budget", value.toLong()).apply()
    }

    fun saveLock(ctx: Context, on: Boolean) {
        lockEnabled = on
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean("lock", on).apply()
    }
}
