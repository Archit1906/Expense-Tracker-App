package com.example.espensestracker.sms

import com.example.espensestracker.data.SOURCE_SMS
import com.example.espensestracker.data.TRANSFER_CATEGORY
import com.example.espensestracker.data.TYPE_CREDIT
import com.example.espensestracker.data.TYPE_REFUND
import com.example.espensestracker.data.TYPE_DEBIT
import com.example.espensestracker.data.Transaction
import com.example.espensestracker.data.UnparsedSms
import java.security.MessageDigest

sealed interface ParseResult {
    data class Ok(val tx: Transaction) : ParseResult
    data class Review(val sms: UnparsedSms) : ParseResult
    data object Ignore : ParseResult
}

/** Pure Kotlin (no Android classes) so it can be unit-tested on the JVM. */
object SmsParser {
    private val I = RegexOption.IGNORE_CASE
    private val amountRx = Regex("""(?:\bRs\.?|\bINR|₹)\s?([\d,]+(?:\.\d{1,2})?)""", I)
    private val debitRx = Regex("""\b(?:debited|spent|paid|sent|withdrawn|purchase)\b""", I)
    private val creditRx = Regex("""\b(?:credited|received|deposited|refund(?:ed)?|revers(?:al|ed))\b""", I)
    private val refundRx = Regex("""\b(?:refund(?:ed)?|revers(?:al|ed))\b""", I)
    private val cardWordRx = Regex("""card""", I)
    // Card-bill payments and own-account moves: money moving between your accounts, not spending.
    private val transferRx = Regex("""credit card (?:bill|payment)|card bill|towards .{0,25}credit card|payment (?:of .{0,20} )?received (?:towards|on|for) (?:your )?.{0,20}card|\bCRED\b|self[- ]?transfer|own account""", I)
    private val strongRx = Regex("""\b(?:debited|credited|spent)\b""", I)
    private val promoRx = Regex("""\b(?:offer|win|apply now|click|pre-approved|loan)\b|https?://|www\.""", I)
    private val hintRx = Regex("""a/c|acct|account|card|upi|vpa|bank""", I)
    private val balRx = Regex("""bal|limit|avl""", I)
    private val accountRx = Regex("""(?:a/c|acct|account|card(?:\s+ending)?|ending)\D{0,12}?(\d{4})(?!\d)""", I)
    private val refRx = Regex("""(?:ref(?:erence)?(?:\s*no)?\.?|rrn|utr|txn\s*(?:id|no))\D{0,5}(\d{9,14})""", I)
    private val vpaRx = Regex("""([\w.\-]{2,}@[A-Za-z]{2,})""")
    private val posRx = Regex("""\bPOS\s*\d{4,}""", I)
    private val foreignRx = Regex("""INTNL|INTERNATIONAL|FOREX""", I)
    private val atmRx = Regex("""\bATM\b""", I)
    private val nameRx = Regex("""\b(?:at|to|towards|info:?)\s+([A-Za-z][A-Za-z0-9 &.'*-]{2,30}?)(?=\s+(?:on|via|ref|using|upi|avl|avail|bal|\d)|[.,;]|$)""", I)
    private val fromRx = Regex("""\bfrom\s+([A-Za-z][A-Za-z .&-]{2,30}?)(?=\s*[.,;]|\s+(?:on|ref|via|upi|credited|to|into|\d)|$)""", I)
    private val phoneSender = Regex("""[+\d\s-]+""")

    private class Rule(pattern: String, val display: String?, val category: String) {
        val rx = Regex(pattern, I)
    }

    // First match wins. display = null -> use the matched keyword, prettified.
    private val RULES = listOf(
        Rule("""\bswiggy""", "Swiggy", "Food & Dining"),
        Rule("""\bzomato""", "Zomato", "Food & Dining"),
        Rule("""domino""", "Domino's Pizza", "Food & Dining"),
        Rule("""mcdonald""", "McDonald's", "Food & Dining"),
        Rule("""tokai""", "Blue Tokai Coffee", "Food & Dining"),
        Rule("""\b(?:starbucks|kfc|burger king|pizza hut|subway)""", null, "Food & Dining"),
        Rule("""\b(?:instamart|blinkit|zepto|bigbasket|dmart)""", null, "Groceries"),
        Rule("""\b(?:uber|rapido|redbus|irctc|makemytrip|indigo|vistara|goibibo|shell|hpcl|bpcl|iocl|ola)\b""", null, "Rides & Travel"),
        Rule("""\b(?:amazon|flipkart|myntra|ajio|meesho|nykaa)""", null, "Shopping"),
        Rule("""\b(?:netflix|spotify|hotstar|youtube|apple|google ?one|jiocinema|kindle)""", null, "Subscriptions"),
        Rule("""\b(?:airtel|vodafone|bsnl|tangedco|tneb|bescom|electricity|broadband|fibernet|jio)\b""", null, "Bills & Utilities"),
        Rule("""\b(?:apollo|pharmeasy|1mg|medplus|netmeds)""", null, "Health"),
        Rule("""\b(?:bookmyshow|pvr|inox)""", null, "Entertainment")
    )

    fun bankName(sender: String): String {
        val s = sender.substringAfter('-').uppercase()
        return listOf("HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "AMEX", "PNB", "BOB", "IDFC", "YES", "CANARA")
            .firstOrNull { s.contains(it) } ?: s
    }

    fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        if (sender.matches(phoneSender)) return ParseResult.Ignore       // personal numbers, not banks
        val dm = debitRx.find(body)
        val cm = creditRx.find(body)
        if (dm == null && cm == null) return ParseResult.Ignore          // OTPs, promos, balance alerts
        if (promoRx.containsMatchIn(body) && !strongRx.containsMatchIn(body)) return ParseResult.Ignore
        if (!hintRx.containsMatchIn(body)) return ParseResult.Ignore

        val amount = amountRx.findAll(body).firstOrNull { m ->
            !body.substring(maxOf(0, m.range.first - 12), m.range.first).contains(balRx)
        }?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return ParseResult.Ignore

        val baseType = if (cm == null || (dm != null && dm.range.first < cm.range.first)) TYPE_DEBIT else TYPE_CREDIT
        val type = if (baseType == TYPE_CREDIT && refundRx.containsMatchIn(body)) TYPE_REFUND else baseType
        val account = accountRx.find(body)?.groupValues?.get(1)
        val ref = refRx.find(body)?.groupValues?.get(1)
        val vpa = vpaRx.find(body)?.groupValues?.get(1)
        val name = (if (type != TYPE_DEBIT) fromRx.find(body)?.groupValues?.get(1) else null)
            ?: nameRx.find(body)?.groupValues?.get(1)
        val hash = sha256("$sender|$timestamp|$body")
        val mkey = keyOf(vpa?.substringBefore('@') ?: posRx.find(body)?.value ?: name)

        fun ok(merchant: String?, category: String) = ParseResult.Ok(
            Transaction(
                amount = amount, type = type, merchant = merchant, category = category,
                account = account, refNo = ref, timestamp = timestamp, source = SOURCE_SMS,
                rawSms = body, sender = sender, smsHash = hash, merchantKey = mkey
            )
        )
        fun review(reason: String) = ParseResult.Review(
            UnparsedSms(
                sender = sender, body = body, timestamp = timestamp, amount = amount,
                account = account, refNo = ref, reason = reason, smsHash = hash, merchantKey = mkey
            )
        )

        val key = vpa?.substringBefore('@') ?: name
        val rule = key?.let { k -> RULES.firstNotNullOfOrNull { r -> r.rx.find(k)?.let { m -> r to m.value } } }
        return when {
            transferRx.containsMatchIn(body) ->
                ok(if (cardWordRx.containsMatchIn(body)) "Credit card payment" else "Transfer", TRANSFER_CATEGORY)
            rule != null -> ok(rule.first.display ?: pretty(rule.second), rule.first.category)
            type != TYPE_DEBIT -> ok((name ?: vpa?.substringBefore('@'))?.let { pretty(it.trim()) }, "Other")
            atmRx.containsMatchIn(body) -> ok("ATM withdrawal", "Other")
            vpa != null -> review("Unmapped VPA handle")
            posRx.containsMatchIn(body) -> review("POS code unmapped")
            foreignRx.containsMatchIn(body) -> review("Foreign merchant")
            name != null -> ok(pretty(name.trim()), "Other")
            else -> review("Unknown merchant")
        }
    }

    /** Stable id for "the same merchant": lowercase letters+digits of the VPA name, POS code or merchant name. */
    fun keyOf(s: String?): String? = s?.lowercase()?.filter { it.isLetterOrDigit() }?.takeIf { it.isNotEmpty() }

    private fun pretty(s: String): String =
        if (s == s.uppercase() || s == s.lowercase())
            s.lowercase().split(' ').filter { it.isNotEmpty() }
                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        else s

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
