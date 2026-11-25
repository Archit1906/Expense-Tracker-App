package com.example.espensestracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.espensestracker.data.AppDatabase
import com.example.espensestracker.util.AppSettings
import com.example.espensestracker.util.Notifier
import com.example.espensestracker.util.weekSummary
import com.example.espensestracker.util.weeklyText
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WeeklySummaryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        AppSettings.load(applicationContext)
        if (AppSettings.weeklyEnabled) postWeeklySummary(applicationContext)
        return Result.success()
    }
}

suspend fun postWeeklySummary(ctx: Context) {
    val all = AppDatabase.get(ctx).transactionDao().getAllOnce()
    val s = weekSummary(all, System.currentTimeMillis(), AppSettings.netCredits)
    Notifier.post(ctx, 2, "Your week in spending", weeklyText(s))
}

object WeeklyScheduler {
    private const val NAME = "weekly_summary"

    /** Every Sunday at 8 pm (approximately: Android may shift it a little to save battery). */
    fun schedule(ctx: Context) {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.WEEK_OF_YEAR, 1)
        }
        val req = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(NAME)
    }
}
