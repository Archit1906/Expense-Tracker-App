package com.example.espensestracker.util

import android.content.Context
import com.example.espensestracker.data.AppDatabase
import com.example.espensestracker.data.TRANSFER_CATEGORY
import com.example.espensestracker.data.TYPE_DEBIT
import com.example.espensestracker.data.Transaction

object UnusualSpend {
    /** Short reason if [amount] is unusually large for this merchant (or overall), else null. */
    fun reason(amount: Double, merchantHistory: List<Double>, overall: List<Double>): String? {
        if (amount < 500) return null
        if (merchantHistory.size >= 3) {
            val avg = merchantHistory.average()
            if (amount >= 2.5 * avg) return "about ${"%.1f".format(amount / avg)}x your usual payment here (${formatMoney(avg, 0)})"
        } else if (overall.size >= 10) {
            val median = overall.sorted()[overall.size / 2]
            if (amount >= 4 * median && amount >= 1000) return "much higher than your typical payment (${formatMoney(median, 0)})"
        }
        return null
    }
}

/** Called for each newly stored live SMS payment. */
suspend fun checkUnusualSpend(ctx: Context, tx: Transaction) {
    if (tx.type != TYPE_DEBIT || tx.category == TRANSFER_CATEGORY) return
    val dao = AppDatabase.get(ctx).transactionDao()
    val merchant = tx.merchantKey?.let { dao.merchantAmounts(it, tx.timestamp) }.orEmpty()
    val overall = dao.recentAmounts(tx.timestamp - 90L * 86_400_000L, tx.timestamp)
    val why = UnusualSpend.reason(tx.amount, merchant, overall) ?: return
    Notifier.post(
        ctx, 1000 + (tx.id % 100_000).toInt(),
        "Unusual payment: ${formatMoney(tx.amount, 0)}", "${tx.merchant ?: tx.category} is $why."
    )
}
