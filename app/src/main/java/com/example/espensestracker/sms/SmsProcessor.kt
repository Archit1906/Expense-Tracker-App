package com.example.espensestracker.sms

import android.content.Context
import android.provider.Telephony
import com.example.espensestracker.data.AppDatabase
import com.example.espensestracker.data.MerchantRule
import com.example.espensestracker.data.SOURCE_SMS
import com.example.espensestracker.data.TYPE_DEBIT
import com.example.espensestracker.data.Transaction
import com.example.espensestracker.data.UnparsedSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Glue between the parser and the database: parse -> learned rules -> dedup -> store. */
class SmsProcessor(context: Context) {
    private val ctx = context.applicationContext
    private val db = AppDatabase.get(ctx)
    private val txDao = db.transactionDao()
    private val unDao = db.unparsedSmsDao()
    private val ruleDao = db.merchantRuleDao()

    /** Returns true if something new was stored. */
    suspend fun process(sender: String, body: String, timestamp: Long): Boolean =
        when (val r = SmsParser.parse(sender, body, timestamp)) {
            is ParseResult.Ok -> store(withRule(r.tx))
            is ParseResult.Review -> {
                val s = r.sms
                val rule = s.merchantKey?.let { ruleDao.get(it) }
                if (rule != null) store(toTx(s, rule.category, rule.displayName))   // learned: skip the queue
                else !isDuplicate(s.smsHash, s.amount, TYPE_DEBIT, s.refNo, s.timestamp, s.account) && unDao.insert(s) > 0
            }
            ParseResult.Ignore -> false
        }

    /** A learned rule beats the built-in merchant list. */
    private suspend fun withRule(t: Transaction): Transaction {
        val rule = t.merchantKey?.let { ruleDao.get(it) } ?: return t
        return t.copy(category = rule.category, merchant = rule.displayName ?: t.merchant)
    }

    private suspend fun store(t: Transaction): Boolean =
        !isDuplicate(t.smsHash!!, t.amount, t.type, t.refNo, t.timestamp, t.account) && txDao.insertIfNew(t) > 0

    /**
     * 1) same SMS hash  2) same ref no + amount (bank SMS and UPI-app SMS for one payment)
     * 3) no ref no: same amount, type and card within 2 minutes.
     */
    private suspend fun isDuplicate(hash: String, amount: Double, type: String, ref: String?, ts: Long, account: String?): Boolean {
        if (txDao.countHash(hash) > 0) return true
        if (ref != null) return txDao.countRef(ref, amount) > 0
        return txDao.countNear(amount, type, ts - 120_000, ts + 120_000, account) > 0
    }

    /** Reads the SMS inbox (last [days] days) through the same pipeline as live messages. */
    suspend fun importInbox(days: Int = 180): Int = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - days * 86_400_000L
        var stored = 0
        ctx.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ?", arrayOf(since.toString()), "${Telephony.Sms.DATE} ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val address = c.getString(0) ?: continue
                val body = c.getString(1) ?: continue
                if (process(address, body, c.getLong(2))) stored++
            }
        }
        stored
    }

    private fun toTx(s: UnparsedSms, category: String, merchant: String?) = Transaction(
        amount = s.amount, type = TYPE_DEBIT, merchant = merchant, category = category,
        account = s.account, refNo = s.refNo, timestamp = s.timestamp, source = SOURCE_SMS,
        rawSms = s.body, sender = s.sender, smsHash = s.smsHash, merchantKey = s.merchantKey
    )

    /** User picked a category for a queued SMS: it becomes a transaction, and the app learns the merchant. */
    suspend fun assign(s: UnparsedSms, category: String, merchant: String?) {
        val name = merchant?.trim()?.ifBlank { null }
        txDao.insertIfNew(toTx(s, category, name))
        unDao.delete(s)
        s.merchantKey?.let { remember(it, category, name) }
    }

    /** Save the rule, fix all past payments to this merchant, and clear matching queue items. */
    suspend fun remember(key: String, category: String, merchant: String?) {
        ruleDao.upsert(MerchantRule(key, category, merchant))
        txDao.applyRule(key, category, merchant)
        unDao.pendingByKey(key).forEach { o ->
            txDao.insertIfNew(toTx(o, category, merchant))
            unDao.delete(o)
        }
    }

    /** One-time: give Phase 1/2 rows a merchantKey (re-parsing the stored raw SMS) so rules can match them. */
    suspend fun backfillKeys() = withContext(Dispatchers.IO) {
        for (t in txDao.missingKeys()) {
            val fromSms = t.rawSms?.let { body ->
                when (val r = SmsParser.parse(t.sender.orEmpty(), body, t.timestamp)) {
                    is ParseResult.Ok -> r.tx.merchantKey
                    is ParseResult.Review -> r.sms.merchantKey
                    ParseResult.Ignore -> null
                }
            }
            val key = fromSms ?: SmsParser.keyOf(t.merchant)
            if (key != null) txDao.update(t.copy(merchantKey = key))
        }
        for (s in unDao.missingKeys()) {
            val key = when (val r = SmsParser.parse(s.sender, s.body, s.timestamp)) {
                is ParseResult.Review -> r.sms.merchantKey
                is ParseResult.Ok -> r.tx.merchantKey
                ParseResult.Ignore -> null
            }
            if (key != null) unDao.setKey(s.id, key)
        }
    }

    /**
     * Re-run the current parser and rules over stored data. Only "Other" transactions and the
     * review queue are touched, so categories you chose yourself are never overwritten.
     */
    suspend fun reparse(): Int = withContext(Dispatchers.IO) {
        var n = 0
        for (t in txDao.otherSms()) {
            val upd: Pair<String, String?>? = when (val r = SmsParser.parse(t.sender.orEmpty(), t.rawSms.orEmpty(), t.timestamp)) {
                is ParseResult.Ok -> withRule(r.tx).let { it.category to it.merchant }
                is ParseResult.Review -> r.sms.merchantKey?.let { ruleDao.get(it) }?.let { it.category to it.displayName }
                ParseResult.Ignore -> null
            }
            if (upd != null && upd.first != "Other") {
                txDao.update(t.copy(category = upd.first, merchant = upd.second ?: t.merchant))
                n++
            }
        }
        for (s in unDao.pendingAll()) {
            if (process(s.sender, s.body, s.timestamp)) { unDao.delete(s); n++ }
        }
        n
    }

    suspend fun dismiss(s: UnparsedSms) = unDao.dismiss(s.id)
    suspend fun dismissAll() = unDao.dismissAll()
}
