package com.example.espensestracker.util

import com.example.espensestracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun csvEscape(s: String?): String {
    val v = s.orEmpty()
    return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
}

/** Raw SMS text is deliberately not exported. */
fun toCsv(list: List<Transaction>): String {
    val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val sb = StringBuilder("date,type,amount,merchant,category,account_last4,reference,note,source,tax_deductible\n")
    list.sortedBy { it.timestamp }.forEach { t ->
        sb.append(
            listOf(
                f.format(Date(t.timestamp)), t.type, String.format(Locale.US, "%.2f", t.amount),
                csvEscape(t.merchant), csvEscape(t.category), t.account.orEmpty(), t.refNo.orEmpty(),
                csvEscape(t.note), t.source, t.taxDeductible.toString()
            ).joinToString(",")
        ).append('\n')
    }
    return sb.toString()
}
