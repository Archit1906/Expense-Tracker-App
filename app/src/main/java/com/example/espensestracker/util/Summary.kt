package com.example.espensestracker.util

import com.example.espensestracker.data.TYPE_DEBIT
import com.example.espensestracker.data.Transaction
import com.example.espensestracker.data.spend

/** Straight-line pace: spent so far / days elapsed * days in the month. */
fun forecastMonthEnd(spentSoFar: Double, day: Int, daysInMonth: Int): Double =
    if (day <= 0) spentSoFar else spentSoFar / day * daysInMonth

fun forecastLine(forecast: Double, budget: Double, day: Int): String {
    if (forecast <= 0) return "No net spending yet this month"
    val vs = if (forecast <= budget) " (${formatMoney(budget - forecast, 0)} under budget)"
    else " (${formatMoney(forecast - budget, 0)} over budget)"
    return "At this pace: ${formatMoney(forecast, 0)} by month end$vs" + if (day < 5) " • early estimate" else ""
}

data class WeekSummary(val spent: Double, val prevSpent: Double, val topCategory: String?, val topAmount: Double, val count: Int)

/** Last 7 days vs the 7 days before that. */
fun weekSummary(list: List<Transaction>, now: Long, netCredits: Boolean): WeekSummary {
    val week = 7 * 86_400_000L
    val cur = list.filter { it.timestamp in (now - week)..now }
    val prev = list.filter { it.timestamp >= now - 2 * week && it.timestamp < now - week }
    val top = cur.filter { it.spend(netCredits) > 0 }.groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.spend(netCredits) } }.maxByOrNull { it.value }
    return WeekSummary(
        cur.sumOf { it.spend(netCredits) }, prev.sumOf { it.spend(netCredits) },
        top?.key, top?.value ?: 0.0, cur.count { it.type == TYPE_DEBIT }
    )
}

fun weeklyText(s: WeekSummary): String {
    val trend = when {
        s.prevSpent <= 0 -> ""
        s.spent <= s.prevSpent -> " (${"%.0f".format((s.prevSpent - s.spent) / s.prevSpent * 100)}% less than the week before)"
        else -> " (${"%.0f".format((s.spent - s.prevSpent) / s.prevSpent * 100)}% more than the week before)"
    }
    val top = s.topCategory?.let { " Biggest category: $it (${formatMoney(s.topAmount, 0)})." } ?: ""
    return "You spent ${formatMoney(s.spent, 0)} over the last 7 days$trend.$top"
}
